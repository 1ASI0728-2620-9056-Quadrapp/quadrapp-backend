"""Publishes simulated sensor readings of the seeded parking lot over MQTT 5 (QoS 1).

Reads the JSON written by tools/seed/seed.py and publishes, per gateway, to
quadrapp/{tenantId}/{gatewayHardwareId}/readings. See tools/simulator/README.md.
"""

from __future__ import annotations

import argparse
import json
import os
import random
import signal
import sys
import time
import urllib.parse
from datetime import datetime, timezone
from pathlib import Path

from simulation import ParkingSimulation, SimulationSettings

TOOLS_DIR = Path(__file__).resolve().parent.parent
DEFAULT_SEED_FILE = TOOLS_DIR / "out" / "seed-output.json"


def env(name: str, default: str | None = None) -> str | None:
    value = os.environ.get(name)
    return value if value not in (None, "") else default


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Publish simulated Quadrapp sensor readings over MQTT.")
    parser.add_argument("--seed-file", type=Path, default=Path(env("SIM_SEED_FILE", str(DEFAULT_SEED_FILE))))
    parser.add_argument("--broker-url", default=env("MQTT_BROKER_URL", "mqtt://127.0.0.1:1883"),
                        help="mqtt://host:port or mqtts://host:port (TLS).")
    parser.add_argument("--username", default=env("MQTT_USERNAME"))
    parser.add_argument("--password", default=env("MQTT_PASSWORD"))
    parser.add_argument("--speed", type=float, default=float(env("SIM_SPEED", "60")),
                        help="Simulated seconds per real second (60: one campus hour per minute).")
    parser.add_argument("--start-hour", type=float, default=float(env("SIM_START_HOUR", "6")))
    parser.add_argument("--end-hour", type=float, default=float(env("SIM_END_HOUR", "22")))
    parser.add_argument("--loop", action="store_true", help="Start the day again after --end-hour.")
    parser.add_argument("--tick", type=float, default=1.0, help="Real seconds between simulation steps.")
    parser.add_argument("--heartbeat-interval", type=float, default=float(env("SIM_HEARTBEAT_S", "120")),
                        help="Real seconds between heartbeats of each device.")
    parser.add_argument("--max-changes-per-tick", type=int, default=2)
    parser.add_argument("--random-seed", type=int, default=None, help="Repeatable runs.")
    parser.add_argument("--dry-run", action="store_true", help="Print the messages instead of publishing them.")
    args = parser.parse_args(argv)
    if args.speed <= 0 or args.tick <= 0 or args.heartbeat_interval <= 0:
        parser.error("--speed, --tick and --heartbeat-interval must be positive.")
    if not 0 <= args.start_hour < args.end_hour <= 24:
        parser.error("Hours must satisfy 0 <= --start-hour < --end-hour <= 24.")
    return args


class Publisher:
    """One MQTT 5 connection per gateway, as each gateway has its own credential (D3)."""

    def __init__(self, broker_url: str, username: str | None, password: str | None, gateways: list[str]):
        import paho.mqtt.client as mqtt
        from paho.mqtt.enums import CallbackAPIVersion

        url = urllib.parse.urlparse(broker_url)
        if url.scheme not in ("mqtt", "mqtts") or not url.hostname:
            raise SystemExit(f"Invalid --broker-url {broker_url!r}: use mqtt://host:port or mqtts://host:port.")
        port = url.port or (8883 if url.scheme == "mqtts" else 1883)

        self.clients = {}
        self.pending = []
        for gateway in gateways:
            client = mqtt.Client(CallbackAPIVersion.VERSION2, client_id=f"quadrapp-sim-{gateway}",
                                 protocol=mqtt.MQTTv5)
            if username:
                client.username_pw_set(username, password)
            if url.scheme == "mqtts":
                client.tls_set()
            client.on_connect = lambda c, u, flags, reason, props, gw=gateway: print(
                f"[{gw}] connected to {url.hostname}:{port}: {reason}", flush=True)
            client.on_disconnect = lambda c, u, flags, reason, props, gw=gateway: print(
                f"[{gw}] disconnected: {reason}", flush=True)
            try:
                client.connect(url.hostname, port, keepalive=60)
            except OSError as error:
                raise SystemExit(f"Cannot connect to the MQTT broker at {url.hostname}:{port}: {error}") from error
            client.loop_start()
            self.clients[gateway] = client

    def publish(self, gateway: str, topic: str, payload: dict) -> None:
        info = self.clients[gateway].publish(topic, json.dumps(payload), qos=1)
        self.pending.append(info)
        self.pending = [item for item in self.pending if not item.is_published()]

    def close(self, timeout: float = 10.0) -> None:
        deadline = time.monotonic() + timeout
        for info in self.pending:
            info.wait_for_publish(max(0.1, deadline - time.monotonic()))
        for client in self.clients.values():
            client.disconnect()
            client.loop_stop()


def gateway_of(topic: str) -> str:
    return topic.split("/")[2]


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        seed = json.loads(args.seed_file.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"Seed file {args.seed_file} not found: run tools/seed/seed.py first.", file=sys.stderr)
        return 1

    rng = random.Random(args.random_seed)
    lot = seed["parkingLot"]
    settings = SimulationSettings(
        occupied_debounce_s=lot["occupiedDebounceS"],
        freed_debounce_s=lot["freedDebounceS"],
        heartbeat_interval_s=args.heartbeat_interval,
        max_changes_per_tick=args.max_changes_per_tick,
    )
    simulation = ParkingSimulation.from_seed(seed, settings, rng, time.monotonic())
    gateways = [gateway["hardwareId"] for gateway in seed["gateways"]]
    publisher = None if args.dry_run else Publisher(args.broker_url, args.username, args.password, gateways)

    stop = False

    def request_stop(*_):
        nonlocal stop
        stop = True

    signal.signal(signal.SIGINT, request_stop)
    signal.signal(signal.SIGTERM, request_stop)

    def emit(message) -> None:
        if publisher is None:
            print(json.dumps({"topic": message.topic, "payload": message.payload}), flush=True)
        else:
            publisher.publish(gateway_of(message.topic), message.topic, message.payload)

    print(f"Simulating {len(simulation.space_sensors)} spaces of '{lot['name']}' (tenant {seed['tenantId']}) "
          f"from {args.start_hour:05.2f}h to {args.end_hour:05.2f}h at x{args.speed:g}.", file=sys.stderr, flush=True)
    started = time.monotonic()
    last_report = -1
    try:
        while not stop:
            now = time.monotonic()
            sim_hour = args.start_hour + (now - started) * args.speed / 3600.0
            if sim_hour >= args.end_hour:
                if not args.loop:
                    break
                started = now
                sim_hour = args.start_hour
            for message in simulation.tick(sim_hour, now, datetime.now(timezone.utc)):
                emit(message)
            if publisher is not None and int(sim_hour * 4) != last_report:
                last_report = int(sim_hour * 4)
                print(f"{int(sim_hour):02d}:{int(sim_hour % 1 * 60):02d} occupied "
                      f"{simulation.occupied_count()}/{len(simulation.space_sensors)} "
                      f"(arrivals {simulation.counters['arrivals']}, departures "
                      f"{simulation.counters['departures']}, readings {simulation.counters['readings']})",
                      flush=True)
            time.sleep(max(0.0, args.tick - (time.monotonic() - now)))
        # The day ended: send the confirmations still due, so the backend keeps the last state.
        while not stop and simulation.has_pending_confirmations():
            for message in simulation.tick(args.end_hour, time.monotonic(), datetime.now(timezone.utc),
                                           allow_changes=False):
                emit(message)
            time.sleep(args.tick)
    finally:
        if publisher is not None:
            publisher.close()
    action = "printed" if publisher is None else "published"
    print(f"Done: {simulation.counters['readings']} readings {action}.", file=sys.stderr, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())

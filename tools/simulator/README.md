# Sensor simulator

`simulator.py` publishes the readings of the parking lot created by `tools/seed/seed.py` to an MQTT broker, as the campus gateway would.

**Sprint 1 scope:** the occupancy follows a synthetic campus weekday curve (`daycurve.py`). Report decisions D2 and D3 say the final simulator replays the City of Melbourne sensor data; that replay is Sprint 2 work.

## What it publishes

- Topic `quadrapp/{tenantId}/{gatewayHardwareId}/readings`, MQTT 5, QoS 1, one connection per gateway.
- One JSON reading per message:

```json
{
  "eventId": "8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10",
  "hardwareId": "demo-ss-n1-01",
  "kind": "SPACE",
  "state": "OCCUPIED",
  "readingAt": "2026-10-09T13:05:12.200Z",
  "battery": 82,
  "signal": -71
}
```

`kind` is `SPACE` (with `state` `FREE` or `OCCUPIED`), `PASS` (with `direction` `IN` or `OUT`) or `HEARTBEAT` (neither). `eventId` is a new UUID v4 per message; `battery` (0-100) and `signal` (RSSI, dBm) are sent with every reading.

## Behaviour

- **Start:** every space sends its initial state, chosen so the share of occupied spaces matches the curve at `--start-hour`.
- **Day curve:** arrivals in the morning peak (07:00-10:00, up to about 93 % at noon), a lunch dip, departures through the afternoon and a small evening group. A slow random noise of up to 4 % and a few swaps (one car leaves, another parks) keep it from looking flat.
- **Confirmed changes:** Sensing confirms a change only when a further reading with the same state arrives after the parking lot's minimum detection time (`occupiedDebounceS` / `freedDebounceS` from the seed file). Every change is therefore sent twice: once when it happens and again 1 s after that time. A space does not change again until its confirmation was sent, and when the day ends the simulator waits for the pending confirmations before it exits.
- **Accesses:** each arrival sends `PASS IN` and each departure `PASS OUT` on one of the access's pass sensors.
- **Heartbeats:** the gateway and every sensor send a `HEARTBEAT` every `--heartbeat-interval` real seconds.
- **Two clocks:** the simulated hour (which runs `--speed` times faster) only drives the curve. `readingAt` is always the real UTC time, because the backend measures the minimum detection time and the reading order on `readingAt`. Readings of one device always have increasing times.

## Run

```bash
python3 -m venv tools/.venv
tools/.venv/bin/pip install -r tools/simulator/requirements.txt
docker compose -f tools/docker-compose.dev.yml up -d     # local Mosquitto (anonymous, dev only)
tools/.venv/bin/python tools/seed/seed.py                # once, writes tools/out/seed-output.json
tools/.venv/bin/python tools/simulator/simulator.py
```

With the defaults a campus day (06:00-22:00) lasts 16 real minutes. Stop it with Ctrl+C.

Watch the messages: `docker compose -f tools/docker-compose.dev.yml exec mosquitto mosquitto_sub -V mqttv5 -t 'quadrapp/#' -v`.

Print the readings without a broker: `tools/.venv/bin/python tools/simulator/simulator.py --dry-run` (JSON lines on stdout).

## Configuration

| Argument | Variable | Default |
| --- | --- | --- |
| `--seed-file` | `SIM_SEED_FILE` | `tools/out/seed-output.json` |
| `--broker-url` | `MQTT_BROKER_URL` | `mqtt://127.0.0.1:1883`; `mqtts://host:8883` uses TLS |
| `--username`, `--password` | `MQTT_USERNAME`, `MQTT_PASSWORD` | none (anonymous) |
| `--speed` | `SIM_SPEED` | `60` simulated seconds per real second |
| `--start-hour`, `--end-hour` | `SIM_START_HOUR`, `SIM_END_HOUR` | `6`, `22` |
| `--loop` | | off; starts the day again after `--end-hour` |
| `--heartbeat-interval` | `SIM_HEARTBEAT_S` | `120` real seconds |
| `--tick` | | `1` real second per step |
| `--max-changes-per-tick` | | `2` |
| `--random-seed` | | random; set it for repeatable runs |
| `--dry-run` | | off |

At high speeds the occupancy lags behind the curve, because each space waits for its confirmation (about 6 s with the seed settings) before it can change again.

## Tests

```bash
cd tools/simulator && python3 -m unittest -v
```

The tests need no broker and no `paho-mqtt`.

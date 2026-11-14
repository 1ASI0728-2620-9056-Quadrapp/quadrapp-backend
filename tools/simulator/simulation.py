"""Occupancy simulation of the seeded parking lot, independent of MQTT.

Every space sensor follows the day curve. A change of state is sent once and, after the
parking lot's minimum detection time, sent again with the same state so that Sensing
confirms it (SensorFeed confirms only on a further matching reading, never from silence).
A space does not change again before its confirmation was sent. Each arrival and departure
also produces a PASS reading (IN/OUT) on one of the access sensors, and every device sends a
HEARTBEAT at a fixed interval.

Two clocks are used: the simulated hour drives the day curve (it runs ``speed`` times faster),
while the reading time ``ts`` is always the real UTC time, because the backend measures the
minimum detection time and the reading order on it.
"""

from __future__ import annotations

import random
from dataclasses import dataclass, field
from datetime import datetime, timedelta

from daycurve import occupancy_target
from payloads import build_reading, reading_topic

# Extra wait after the minimum detection time before the confirming reading.
CONFIRMATION_MARGIN_S = 1.0


@dataclass
class Message:
    topic: str
    payload: dict


@dataclass
class Device:
    hardware_id: str
    gateway_hardware_id: str
    battery: float
    next_heartbeat: float
    last_reading_at: datetime | None = None


@dataclass
class SpaceSensor(Device):
    label: str = ""
    occupied: bool | None = None
    confirm_at: float | None = None


@dataclass
class SimulationSettings:
    occupied_debounce_s: int
    freed_debounce_s: int
    heartbeat_interval_s: float = 120.0
    max_changes_per_tick: int = 2
    churn_probability: float = 0.15
    noise_step: float = 0.004
    noise_limit: float = 0.04


@dataclass
class ParkingSimulation:
    tenant_id: int
    space_sensors: list[SpaceSensor]
    pass_sensors: list[Device]
    gateways: list[Device]
    settings: SimulationSettings
    rng: random.Random
    noise: float = 0.0
    started: bool = False
    counters: dict = field(default_factory=lambda: {"arrivals": 0, "departures": 0, "readings": 0})

    @classmethod
    def from_seed(cls, seed: dict, settings: SimulationSettings | None, rng: random.Random,
                  now: float) -> "ParkingSimulation":
        lot = seed["parkingLot"]
        settings = settings or SimulationSettings(lot["occupiedDebounceS"], lot["freedDebounceS"])

        def device(kind, item, gateway):
            return kind(hardware_id=item["hardwareId"], gateway_hardware_id=gateway,
                        battery=rng.uniform(70, 100),
                        next_heartbeat=now + rng.uniform(0, settings.heartbeat_interval_s))

        spaces = []
        for item in seed["spaceSensors"]:
            sensor = device(SpaceSensor, item, item["gatewayHardwareId"])
            sensor.label = item["spaceLabel"]
            spaces.append(sensor)
        if not spaces:
            raise ValueError("The seed file has no space sensors.")
        return cls(
            tenant_id=int(seed["tenantId"]),
            space_sensors=spaces,
            pass_sensors=[device(Device, item, item["gatewayHardwareId"]) for item in seed.get("passSensors", [])],
            gateways=[device(Device, item, item["hardwareId"]) for item in seed["gateways"]],
            settings=settings,
            rng=rng,
        )

    # -- readings -----------------------------------------------------------------------------

    def _reading(self, sensor: Device, kind: str, wall: datetime, **fields) -> Message:
        # The backend drops a reading that is not later than the previous one of the device.
        reading_at = wall
        if sensor.last_reading_at is not None and reading_at <= sensor.last_reading_at:
            reading_at = sensor.last_reading_at + timedelta(milliseconds=1)
        sensor.last_reading_at = reading_at
        self.counters["readings"] += 1
        payload = build_reading(sensor.hardware_id, kind, reading_at,
                                battery=round(sensor.battery), rssi=self.rng.randint(-88, -55), **fields)
        return Message(reading_topic(self.tenant_id, sensor.gateway_hardware_id), payload)

    def _set_space(self, sensor: SpaceSensor, occupied: bool, now: float, wall: datetime,
                   out: list[Message], with_passage: bool) -> None:
        sensor.occupied = occupied
        debounce = self.settings.occupied_debounce_s if occupied else self.settings.freed_debounce_s
        sensor.confirm_at = now + debounce + CONFIRMATION_MARGIN_S
        out.append(self._reading(sensor, "SPACE", wall, state="OCCUPIED" if occupied else "FREE"))
        if with_passage:
            self.counters["arrivals" if occupied else "departures"] += 1
            if self.pass_sensors:
                gate = self.rng.choice(self.pass_sensors)
                out.append(self._reading(gate, "PASS", wall, direction="IN" if occupied else "OUT"))

    # -- tick ---------------------------------------------------------------------------------

    def target_occupied(self, sim_hour: float) -> int:
        share = min(1.0, max(0.0, occupancy_target(sim_hour) + self.noise))
        return round(share * len(self.space_sensors))

    def occupied_count(self) -> int:
        return sum(1 for sensor in self.space_sensors if sensor.occupied)

    def _follow_curve(self, sim_hour: float, now: float, wall: datetime, out: list[Message]) -> None:
        if not self.started:
            # Initial state of every space, as a sensor reports when it starts.
            self.started = True
            occupied = set(self.rng.sample(range(len(self.space_sensors)), self.target_occupied(sim_hour)))
            for index, sensor in enumerate(self.space_sensors):
                self._set_space(sensor, index in occupied, now, wall, out, with_passage=False)
            return

        self.noise = max(-self.settings.noise_limit,
                         min(self.settings.noise_limit, self.noise + self.rng.gauss(0, self.settings.noise_step)))
        ready = [sensor for sensor in self.space_sensors if sensor.confirm_at is None]
        free = [sensor for sensor in ready if not sensor.occupied]
        taken = [sensor for sensor in ready if sensor.occupied]
        delta = self.target_occupied(sim_hour) - self.occupied_count()
        if delta > 0:
            for sensor in self.rng.sample(free, min(delta, len(free), self.settings.max_changes_per_tick)):
                self._set_space(sensor, True, now, wall, out, with_passage=True)
        elif delta < 0:
            for sensor in self.rng.sample(taken, min(-delta, len(taken), self.settings.max_changes_per_tick)):
                self._set_space(sensor, False, now, wall, out, with_passage=True)
        elif free and taken and self.rng.random() < self.settings.churn_probability:
            # Someone leaves and someone else parks: the total stays, the spaces change.
            self._set_space(self.rng.choice(taken), False, now, wall, out, with_passage=True)
            self._set_space(self.rng.choice(free), True, now, wall, out, with_passage=True)

    def has_pending_confirmations(self) -> bool:
        return any(sensor.confirm_at is not None for sensor in self.space_sensors)

    def tick(self, sim_hour: float, now: float, wall: datetime, allow_changes: bool = True) -> list[Message]:
        """Advances the simulation. ``now`` is a monotonic clock in seconds, ``wall`` real UTC time.

        With ``allow_changes`` False only confirmations and heartbeats are sent (used to finish).
        """
        out: list[Message] = []

        # Confirm the changes whose minimum detection time has passed.
        for sensor in self.space_sensors:
            if sensor.confirm_at is not None and now >= sensor.confirm_at:
                sensor.confirm_at = None
                out.append(self._reading(sensor, "SPACE", wall,
                                         state="OCCUPIED" if sensor.occupied else "FREE"))

        if allow_changes:
            self._follow_curve(sim_hour, now, wall, out)

        # Heartbeats keep every device from being reported as silent.
        for sensor in [*self.gateways, *self.space_sensors, *self.pass_sensors]:
            if now >= sensor.next_heartbeat:
                sensor.next_heartbeat = now + self.settings.heartbeat_interval_s
                sensor.battery = max(0.0, sensor.battery - 0.05)
                out.append(self._reading(sensor, "HEARTBEAT", wall))
        return out

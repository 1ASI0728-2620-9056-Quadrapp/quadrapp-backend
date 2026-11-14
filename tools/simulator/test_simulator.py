import random
import unittest
import uuid
from datetime import datetime, timedelta, timezone

from daycurve import CAMPUS_WEEKDAY, occupancy_target
from payloads import build_reading, format_instant, reading_topic
from simulation import ParkingSimulation, SimulationSettings

AT = datetime(2026, 10, 9, 13, 5, 12, 200_000, tzinfo=timezone.utc)


class ReadingPayloadTest(unittest.TestCase):
    def test_space_reading_has_the_contract_fields(self):
        reading = build_reading("demo-ss-n1-01", "SPACE", AT, state="OCCUPIED", battery=82, rssi=-71)
        self.assertEqual(set(reading), {"eventId", "sensorId", "kind", "state", "ts", "battery", "rssi"})
        self.assertEqual(reading["ts"], "2026-10-09T13:05:12.200Z")
        self.assertEqual(reading["sensorId"], "demo-ss-n1-01")
        self.assertEqual((reading["kind"], reading["state"], reading["battery"], reading["rssi"]),
                         ("SPACE", "OCCUPIED", 82, -71))
        uuid.UUID(reading["eventId"], version=4)

    def test_pass_reading_carries_direction_only(self):
        reading = build_reading("demo-ps-01", "PASS", AT, direction="IN")
        self.assertEqual(reading["direction"], "IN")
        self.assertNotIn("state", reading)
        self.assertNotIn("battery", reading)
        self.assertNotIn("rssi", reading)

    def test_heartbeat_carries_neither_state_nor_direction(self):
        reading = build_reading("demo-gw-01", "HEARTBEAT", AT, battery=90)
        self.assertNotIn("state", reading)
        self.assertNotIn("direction", reading)

    def test_rejects_readings_the_backend_would_reject(self):
        invalid = [
            dict(kind="SPACE", direction="IN", state="FREE"),
            dict(kind="SPACE"),
            dict(kind="PASS", state="FREE"),
            dict(kind="HEARTBEAT", state="FREE"),
            dict(kind="SPACE", state="UNKNOWN"),
            dict(kind="SPACE", state="FREE", battery=101),
            dict(kind="OTHER"),
        ]
        for fields in invalid:
            with self.subTest(fields=fields), self.assertRaises(ValueError):
                build_reading("demo-ss-n1-01", reading_at=AT, **fields)

    def test_reading_time_is_converted_to_utc(self):
        lima = timezone(timedelta(hours=-5))
        self.assertEqual(format_instant(datetime(2026, 10, 9, 8, 0, tzinfo=lima)), "2026-10-09T13:00:00.000Z")
        with self.assertRaises(ValueError):
            format_instant(datetime(2026, 10, 9, 8, 0))

    def test_topic_follows_the_contract(self):
        self.assertEqual(reading_topic(7, "demo-gw-01"), "quadrapp/7/demo-gw-01/readings")
        with self.assertRaises(ValueError):
            reading_topic(7, "gw/1")


class DayCurveTest(unittest.TestCase):
    def test_morning_peak_and_afternoon_departures(self):
        self.assertLess(occupancy_target(6), 0.1)
        self.assertGreater(occupancy_target(10.5), 0.85)
        self.assertGreater(occupancy_target(10), occupancy_target(8))
        self.assertGreater(occupancy_target(14), occupancy_target(18))
        self.assertLess(occupancy_target(22.5), 0.1)

    def test_interpolates_between_points_and_wraps_the_day(self):
        self.assertAlmostEqual(occupancy_target(7.5), (0.20 + 0.60) / 2)
        self.assertAlmostEqual(occupancy_target(34), occupancy_target(10))

    def test_every_value_is_a_share(self):
        for hour in range(0, 240):
            self.assertTrue(0.0 <= occupancy_target(hour / 10) <= 1.0)
        self.assertEqual(CAMPUS_WEEKDAY[0][0], 0.0)
        self.assertEqual(CAMPUS_WEEKDAY[-1][0], 24.0)


def seed(spaces=20):
    return {
        "tenantId": 3,
        "parkingLot": {"name": "Demo", "occupiedDebounceS": 5, "freedDebounceS": 7},
        "gateways": [{"id": 1, "hardwareId": "demo-gw-01"}],
        "spaceSensors": [{"id": 10 + i, "hardwareId": f"demo-ss-{i}", "spaceLabel": f"N1-{i:02d}",
                          "gatewayHardwareId": "demo-gw-01"} for i in range(spaces)],
        "passSensors": [{"id": 99, "hardwareId": "demo-ps-01", "gatewayHardwareId": "demo-gw-01"}],
    }


class SimulationTest(unittest.TestCase):
    def setUp(self):
        settings = SimulationSettings(occupied_debounce_s=5, freed_debounce_s=7, heartbeat_interval_s=10_000,
                                      max_changes_per_tick=20, churn_probability=0, noise_step=0)
        self.sim = ParkingSimulation.from_seed(seed(), settings, random.Random(1), now=0.0)
        for sensor in [*self.sim.gateways, *self.sim.space_sensors, *self.sim.pass_sensors]:
            sensor.next_heartbeat = 10_000

    def spaces(self, messages):
        return [m.payload for m in messages if m.payload["kind"] == "SPACE"]

    def test_initial_state_follows_the_curve_and_every_change_is_confirmed_after_the_debounce(self):
        first = self.sim.tick(10.0, now=0.0, wall=AT)
        self.assertEqual(len(self.spaces(first)), 20)
        self.assertEqual(self.sim.occupied_count(), round(occupancy_target(10.0) * 20))
        self.assertTrue(all(m.topic == "quadrapp/3/demo-gw-01/readings" for m in first))

        # Too early: nothing is confirmed and no space changes while its confirmation is pending.
        self.assertEqual(self.spaces(self.sim.tick(10.0, now=5.5, wall=AT + timedelta(seconds=5.5))), [])

        occupied_confirm = self.spaces(self.sim.tick(10.0, now=6.0, wall=AT + timedelta(seconds=6)))
        self.assertTrue(occupied_confirm)
        self.assertTrue(all(p["state"] == "OCCUPIED" for p in occupied_confirm))

        freed_confirm = self.spaces(self.sim.tick(10.0, now=8.0, wall=AT + timedelta(seconds=8)))
        self.assertTrue(freed_confirm)
        self.assertTrue(all(p["state"] == "FREE" for p in freed_confirm))
        self.assertEqual(len(occupied_confirm) + len(freed_confirm), 20)

    def test_departures_send_free_readings_and_out_passages(self):
        self.sim.tick(10.0, now=0.0, wall=AT)
        self.sim.tick(10.0, now=8.0, wall=AT + timedelta(seconds=8))
        before = self.sim.occupied_count()
        messages = self.sim.tick(21.5, now=9.0, wall=AT + timedelta(seconds=9))
        freed = [p for p in self.spaces(messages) if p["state"] == "FREE"]
        passes = [m.payload for m in messages if m.payload["kind"] == "PASS"]
        self.assertEqual(len(freed), before - self.sim.occupied_count())
        self.assertGreater(len(freed), 0)
        self.assertEqual([p["direction"] for p in passes], ["OUT"] * len(freed))

    def test_finishing_sends_only_the_pending_confirmations(self):
        self.sim.tick(10.0, now=0.0, wall=AT)
        self.assertTrue(self.sim.has_pending_confirmations())
        before = self.sim.occupied_count()
        messages = self.sim.tick(21.5, now=8.0, wall=AT + timedelta(seconds=8), allow_changes=False)
        self.assertEqual(len(self.spaces(messages)), 20)
        self.assertEqual(self.sim.occupied_count(), before)
        self.assertFalse(self.sim.has_pending_confirmations())

    def test_reading_times_of_a_device_always_increase(self):
        self.sim.tick(10.0, now=0.0, wall=AT)
        self.sim.tick(10.0, now=8.0, wall=AT)  # same wall time as the first reading
        times = {}
        for sensor in self.sim.space_sensors:
            times[sensor.hardware_id] = sensor.last_reading_at
        self.assertTrue(all(value > AT for value in times.values()))


if __name__ == "__main__":
    unittest.main()

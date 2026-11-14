"""Reading messages published by a gateway (report 5.3.1 SensorReading, 5.3.2 MqttInboundListener)."""

from __future__ import annotations

import uuid
from datetime import datetime, timezone

KINDS = ("SPACE", "PASS", "HEARTBEAT")
STATES = ("FREE", "OCCUPIED")
DIRECTIONS = ("IN", "OUT")


def format_instant(moment: datetime) -> str:
    """ISO-8601 in UTC with milliseconds and a trailing Z, e.g. 2026-10-09T13:05:12.200Z."""
    if moment.tzinfo is None:
        raise ValueError("readingAt must be timezone-aware.")
    utc = moment.astimezone(timezone.utc)
    return utc.strftime("%Y-%m-%dT%H:%M:%S.") + f"{utc.microsecond // 1000:03d}Z"


def reading_topic(tenant_id: int, gateway_hardware_id: str) -> str:
    """quadrapp/{tenantId}/{gatewayHardwareId}/readings"""
    if not isinstance(tenant_id, int) or tenant_id <= 0:
        raise ValueError("tenantId must be a positive integer.")
    if not gateway_hardware_id or any(char in gateway_hardware_id for char in "/+#"):
        raise ValueError("gateway hardware id cannot be empty or contain '/', '+' or '#'.")
    return f"quadrapp/{tenant_id}/{gateway_hardware_id}/readings"


def build_reading(
    hardware_id: str,
    kind: str,
    reading_at: datetime,
    *,
    state: str | None = None,
    direction: str | None = None,
    battery: int | None = None,
    signal: int | None = None,
    event_id: str | None = None,
) -> dict:
    """Builds one reading with the same rules the backend applies to SensorReading.

    SPACE carries a state and no direction, PASS a direction and no state, HEARTBEAT neither.
    battery (0-100) and signal (RSSI in dBm) are optional and left out when None.
    """
    if not hardware_id or not hardware_id.strip() or len(hardware_id.strip()) > 80:
        raise ValueError("hardwareId is required and must not exceed 80 characters.")
    if kind not in KINDS:
        raise ValueError(f"kind must be one of {KINDS}.")
    if kind == "SPACE" and (state not in STATES or direction is not None):
        raise ValueError("SPACE requires state FREE or OCCUPIED and no direction.")
    if kind == "PASS" and (direction not in DIRECTIONS or state is not None):
        raise ValueError("PASS requires direction IN or OUT and no state.")
    if kind == "HEARTBEAT" and (state is not None or direction is not None):
        raise ValueError("HEARTBEAT cannot carry state or direction.")
    if battery is not None and not 0 <= battery <= 100:
        raise ValueError("battery must be between 0 and 100.")

    reading = {
        "eventId": event_id or str(uuid.uuid4()),
        "hardwareId": hardware_id.strip(),
        "kind": kind,
        "readingAt": format_instant(reading_at),
    }
    if state is not None:
        reading["state"] = state
    if direction is not None:
        reading["direction"] = direction
    if battery is not None:
        reading["battery"] = int(battery)
    if signal is not None:
        reading["signal"] = int(signal)
    return reading

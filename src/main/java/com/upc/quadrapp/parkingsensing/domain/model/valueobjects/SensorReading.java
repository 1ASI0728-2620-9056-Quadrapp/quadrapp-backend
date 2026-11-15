package com.upc.quadrapp.parkingsensing.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

public record SensorReading(
        String eventId,
        String hardwareId,
        ReadingKind kind,
        SensorState state,
        PassDirection direction,
        Instant readingAt,
        Integer batteryPercent,
        Integer signalRssi,
        String gatewayHardwareId
) {
    public SensorReading {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required.");
        }
        eventId = eventId.trim();
        hardwareId = new HardwareIdentifier(hardwareId).value();
        Objects.requireNonNull(kind, "kind is required.");
        Objects.requireNonNull(readingAt, "readingAt is required.");
        if (kind == ReadingKind.SPACE && (state == null || direction != null)) {
            throw new IllegalArgumentException("SPACE requires state and no direction.");
        }
        if (kind == ReadingKind.PASS && (direction == null || state != null)) {
            throw new IllegalArgumentException("PASS requires direction and no state.");
        }
        if (kind == ReadingKind.HEARTBEAT && (state != null || direction != null)) {
            throw new IllegalArgumentException("HEARTBEAT cannot carry state or direction.");
        }
        if (batteryPercent != null && (batteryPercent < 0 || batteryPercent > 100)) {
            throw new IllegalArgumentException("batteryPercent must be between 0 and 100.");
        }
        if (gatewayHardwareId != null) {
            gatewayHardwareId = new HardwareIdentifier(gatewayHardwareId).value();
        }
    }
}

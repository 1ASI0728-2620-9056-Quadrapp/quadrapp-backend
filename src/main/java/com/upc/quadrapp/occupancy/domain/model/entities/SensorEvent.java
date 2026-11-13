package com.upc.quadrapp.occupancy.domain.model.entities;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;

import java.time.Instant;
import java.util.Objects;

public record SensorEvent(
        String eventId,
        Long deviceId,
        Long spaceId,
        Long accessId,
        OccupancyEventType eventType,
        Instant occurredAt,
        Instant receivedAt
) {
    public SensorEvent {
        if (eventId == null || eventId.isBlank() || eventId.length() > 100) {
            throw new IllegalArgumentException("Event ID must contain 1 to 100 characters.");
        }
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("Device ID must be positive.");
        }
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(receivedAt);

        boolean spaceEvent = eventType == OccupancyEventType.OCCUPIED
                || eventType == OccupancyEventType.FREED;
        if (spaceEvent != (spaceId != null && accessId == null)) {
            throw new IllegalArgumentException(
                    "Space events require only a space ID."
            );
        }
        if (!spaceEvent && !(accessId != null && spaceId == null)) {
            throw new IllegalArgumentException(
                    "Passage events require only an access ID."
            );
        }
        if ((spaceId != null && spaceId <= 0) || (accessId != null && accessId <= 0)) {
            throw new IllegalArgumentException("Destination IDs must be positive.");
        }
    }
}

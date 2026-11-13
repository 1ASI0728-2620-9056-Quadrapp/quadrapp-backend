package com.upc.quadrapp.occupancy.domain.model.commands;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;

import java.time.Instant;
import java.util.Objects;

public record RecordSensorEventCommand(
        Long institutionId,
        Long parkingLotId,
        Long deviceId,
        Long spaceId,
        Long accessId,
        String eventId,
        OccupancyEventType eventType,
        Instant occurredAt
) {
    public RecordSensorEventCommand {
        if (institutionId == null || institutionId <= 0
                || parkingLotId == null || parkingLotId <= 0
                || deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException(
                    "Institution, parking lot, and device IDs must be positive."
            );
        }
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(occurredAt);
    }
}

package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

import java.time.Instant;

public record ScheduleCalendarEventCommand(
        Long institutionId,
        Long parkingLotId,
        Long zoneId,
        Long createdBy,
        String name,
        Instant startAt,
        Instant endAt
) {
    public ScheduleCalendarEventCommand {

        if (institutionId == null || institutionId <= 0) {
            throw new IllegalArgumentException(
                    "institutionId must be positive."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name is required."
            );
        }

        if (parkingLotId != null && parkingLotId <= 0) {
            throw new IllegalArgumentException(
                    "parkingLotId must be positive."
            );
        }

        if (zoneId != null && zoneId <= 0) {
            throw new IllegalArgumentException(
                    "zoneId must be positive."
            );
        }

        if (createdBy != null && createdBy <= 0) {
            throw new IllegalArgumentException(
                    "createdBy must be positive."
            );
        }

        if (zoneId != null && parkingLotId == null) {
            throw new IllegalArgumentException(
                    "zoneId requires parkingLotId."
            );
        }

        new EventWindow(startAt, endAt);
    }
}

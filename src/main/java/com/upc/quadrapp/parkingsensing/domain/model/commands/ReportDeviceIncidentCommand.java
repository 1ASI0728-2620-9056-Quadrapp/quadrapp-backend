package com.upc.quadrapp.parkingsensing.domain.model.commands;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentCategory;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;

import java.time.Instant;

public record ReportDeviceIncidentCommand(
        Long institutionId,
        Long parkingLotId,
        Long deviceId,
        String hardwareId,
        IncidentType type,
        IncidentCategory category,
        Long reportedBy,
        String details,
        Instant occurredAt
) {
    public ReportDeviceIncidentCommand {
        if (institutionId == null || institutionId <= 0 ||
                parkingLotId == null || parkingLotId <= 0 ||
                type == null || occurredAt == null) {
            throw new IllegalArgumentException(
                    "Invalid incident report."
            );
        }
    }
}

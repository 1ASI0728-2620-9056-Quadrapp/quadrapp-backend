package com.upc.quadrapp.parkingsensing.domain.model.commands;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import java.time.Instant;
import java.util.Objects;

public record ReportDeviceIncidentCommand(Long institutionId, Long parkingLotId, Long deviceId,
                                          String hardwareId, IncidentType type,
                                          String details, Instant occurredAt) {
    public ReportDeviceIncidentCommand {
        if (institutionId == null || institutionId <= 0) throw new IllegalArgumentException("institutionId must be positive.");
        if (parkingLotId != null && parkingLotId <= 0) throw new IllegalArgumentException("parkingLotId must be positive.");
        if (deviceId != null && deviceId <= 0) throw new IllegalArgumentException("deviceId must be positive.");
        hardwareId = new HardwareIdentifier(hardwareId).value();
        Objects.requireNonNull(type, "type is required.");
        Objects.requireNonNull(occurredAt, "occurredAt is required.");
    }
}

package com.upc.quadrapp.parkingsensing.domain.model.commands;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import java.util.Objects;

public record RegisterDeviceCommand(Long institutionId, Long parkingLotId,
                                    String hardwareId, DeviceType type, int reportIntervalSeconds) {
    public RegisterDeviceCommand {
        if (institutionId == null || institutionId <= 0) throw new IllegalArgumentException("institutionId must be positive.");
        if (parkingLotId == null || parkingLotId <= 0) throw new IllegalArgumentException("parkingLotId must be positive.");
        hardwareId = new HardwareIdentifier(hardwareId).value();
        Objects.requireNonNull(type, "type is required.");
        if (reportIntervalSeconds <= 0) throw new IllegalArgumentException("reportIntervalSeconds must be positive.");
    }
}

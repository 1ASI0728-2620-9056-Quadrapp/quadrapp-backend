package com.upc.quadrapp.parkingsensing.domain.model.commands;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;

public record RegisterDeviceCommand(
        Long institutionId,
        Long parkingLotId,
        String hardwareId,
        DeviceType type,
        int reportIntervalSeconds,
        Long gatewayId,
        Boolean directional,
        String firmwareVersion,
        Long registeredBy
) {
    public RegisterDeviceCommand {
        if (institutionId == null || institutionId <= 0 ||
                parkingLotId == null || parkingLotId <= 0 ||
                hardwareId == null || hardwareId.isBlank() ||
                type == null || reportIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Invalid device registration."
            );
        }

        if (gatewayId != null && gatewayId <= 0) {
            throw new IllegalArgumentException(
                    "gatewayId must be positive."
            );
        }

        if (registeredBy != null && registeredBy <= 0) {
            throw new IllegalArgumentException(
                    "registeredBy must be positive."
            );
        }
    }
}

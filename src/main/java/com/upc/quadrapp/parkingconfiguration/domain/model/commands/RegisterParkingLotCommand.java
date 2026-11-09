package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record RegisterParkingLotCommand(Long institutionId, String name) {
    public RegisterParkingLotCommand {
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
    }
}

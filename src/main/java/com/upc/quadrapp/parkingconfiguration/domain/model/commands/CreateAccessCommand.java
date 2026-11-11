package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CreateAccessCommand(Long parkingLotId, String name) {
    public CreateAccessCommand {
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException(
                    "parkingLotId must be positive."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name is required."
            );
        }
    }
}

package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CreateZoneCommand(Long parkingLotId, String name) {
    public CreateZoneCommand {
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

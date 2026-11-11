package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CreateLevelCommand(Long zoneId, String name) {
    public CreateLevelCommand {
        if (zoneId == null || zoneId <= 0) {
            throw new IllegalArgumentException(
                    "zoneId must be positive."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name is required."
            );
        }
    }
}

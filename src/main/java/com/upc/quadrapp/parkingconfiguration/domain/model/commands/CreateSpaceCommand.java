package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CreateSpaceCommand(Long levelId, String label) {
    public CreateSpaceCommand {
        if (levelId == null || levelId <= 0) {
            throw new IllegalArgumentException(
                    "levelId must be positive."
            );
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    "label is required."
            );
        }
    }
}

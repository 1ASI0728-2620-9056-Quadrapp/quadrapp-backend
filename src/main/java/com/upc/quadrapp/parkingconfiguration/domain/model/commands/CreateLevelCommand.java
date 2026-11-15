package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CreateLevelCommand(Long zoneId, String name, Integer sortOrder)
{
    public CreateLevelCommand {
        if (zoneId == null || zoneId <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid level input.");
        }
    }
}

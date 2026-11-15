package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CreateZoneCommand(Long parkingLotId, String name, String description, Integer sortOrder)
{
    public CreateZoneCommand {
        if (parkingLotId == null || parkingLotId <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid zone input.");
        }
    }
}

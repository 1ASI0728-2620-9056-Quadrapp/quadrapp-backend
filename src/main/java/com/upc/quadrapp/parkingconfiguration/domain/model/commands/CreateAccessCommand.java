package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.AccessType;

public record CreateAccessCommand(Long parkingLotId, String name, AccessType type, Boolean flowCountingEnabled)
{
    public CreateAccessCommand {
        if (parkingLotId == null || parkingLotId <= 0 || name == null || name.isBlank() || type == null) {

            throw new IllegalArgumentException("Parking lot, name and access type are required.");
        }
    }
}

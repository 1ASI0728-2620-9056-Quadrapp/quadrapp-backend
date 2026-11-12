package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceType;

import java.math.BigDecimal;

public record CreateSpaceCommand(Long levelId, String label, SpaceType type, BigDecimal posX, BigDecimal posY)
{
    public CreateSpaceCommand {
        if (levelId == null || levelId <= 0 || label == null || label.isBlank()) {
            throw new IllegalArgumentException("Invalid space input.");
        }
    }
}

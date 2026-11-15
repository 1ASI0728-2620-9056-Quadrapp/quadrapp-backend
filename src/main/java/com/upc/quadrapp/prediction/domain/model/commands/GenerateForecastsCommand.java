package com.upc.quadrapp.prediction.domain.model.commands;

public record GenerateForecastsCommand(Long parkingLotId) {
    public GenerateForecastsCommand {
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException("Parking lot ID must be positive.");
        }
    }
}

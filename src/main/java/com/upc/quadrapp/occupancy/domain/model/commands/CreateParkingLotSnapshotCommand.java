package com.upc.quadrapp.occupancy.domain.model.commands;

public record CreateParkingLotSnapshotCommand(Long parkingLotId) {
    public CreateParkingLotSnapshotCommand {
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException("Parking lot ID must be positive.");
        }
    }
}

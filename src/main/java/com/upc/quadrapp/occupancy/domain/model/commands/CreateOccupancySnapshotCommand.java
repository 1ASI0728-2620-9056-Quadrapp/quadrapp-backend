package com.upc.quadrapp.occupancy.domain.model.commands;

public record CreateOccupancySnapshotCommand(Long zoneId) {
    public CreateOccupancySnapshotCommand {
        if (zoneId == null || zoneId <= 0) {
            throw new IllegalArgumentException("Zone ID must be positive.");
        }
    }
}

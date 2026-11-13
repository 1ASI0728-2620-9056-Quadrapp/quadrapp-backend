package com.upc.quadrapp.occupancy.interfaces.rest.resources;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;

import java.time.Instant;

public record OccupancySnapshotResource(
        Long id,
        Long zoneId,
        Instant takenAt,
        long totalSpaces,
        long freeCount,
        long occupiedCount,
        long unknownCount
) {
    public static OccupancySnapshotResource from(OccupancySnapshot snapshot) {
        return new OccupancySnapshotResource(
                snapshot.id(),
                snapshot.zoneId(),
                snapshot.takenAt(),
                snapshot.counts().totalSpaces(),
                snapshot.counts().freeCount(),
                snapshot.counts().occupiedCount(),
                snapshot.counts().unknownCount()
        );
    }
}

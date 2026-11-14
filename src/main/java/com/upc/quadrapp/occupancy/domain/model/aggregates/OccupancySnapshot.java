package com.upc.quadrapp.occupancy.domain.model.aggregates;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;

import java.time.Instant;
import java.util.Objects;

/**
 * Availability of one zone at one moment. Snapshots saved before Occupancy owned its state have
 * no parking lot.
 */
public record OccupancySnapshot(
        Long id,
        Long parkingLotId,
        Long zoneId,
        Instant takenAt,
        OccupancyCounts counts
) {
    public OccupancySnapshot {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Snapshot ID must be positive.");
        }
        if (zoneId == null || zoneId <= 0) {
            throw new IllegalArgumentException("Zone ID must be positive.");
        }
        Objects.requireNonNull(takenAt);
        Objects.requireNonNull(counts);
        counts.requireSmallintRange();
    }

    public static OccupancySnapshot create(
            Long parkingLotId,
            Long zoneId,
            Instant takenAt,
            OccupancyCounts counts
    ) {
        return new OccupancySnapshot(null, parkingLotId, zoneId, takenAt, counts);
    }
}

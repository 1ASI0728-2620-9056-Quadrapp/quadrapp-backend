package com.upc.quadrapp.occupancy.domain.model.valueobjects;

import java.util.Objects;

/** States of the active spaces of one zone, counted from the stored space states. */
public record ZoneCount(Long zoneId, OccupancyCounts counts) {
    public ZoneCount {
        if (zoneId == null || zoneId <= 0) {
            throw new IllegalArgumentException("Zone ID must be positive.");
        }
        Objects.requireNonNull(counts);
    }
}

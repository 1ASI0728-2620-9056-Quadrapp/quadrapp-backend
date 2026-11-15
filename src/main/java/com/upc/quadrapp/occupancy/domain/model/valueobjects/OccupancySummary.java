package com.upc.quadrapp.occupancy.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

public record OccupancySummary(
        OccupancyScopeType scopeType,
        Long scopeId,
        Instant takenAt,
        OccupancyCounts counts
) {
    public OccupancySummary {
        Objects.requireNonNull(scopeType);
        if (scopeId == null || scopeId <= 0) {
            throw new IllegalArgumentException("Scope ID must be positive.");
        }
        Objects.requireNonNull(takenAt);
        Objects.requireNonNull(counts);
    }
}

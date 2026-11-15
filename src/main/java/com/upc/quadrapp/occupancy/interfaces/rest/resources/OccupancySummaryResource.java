package com.upc.quadrapp.occupancy.interfaces.rest.resources;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;

import java.time.Instant;

public record OccupancySummaryResource(
        String scopeType,
        Long scopeId,
        Instant takenAt,
        long totalSpaces,
        long freeCount,
        long occupiedCount,
        long unknownCount
) {
    public static OccupancySummaryResource from(OccupancySummary summary) {
        return new OccupancySummaryResource(
                summary.scopeType().name(),
                summary.scopeId(),
                summary.takenAt(),
                summary.counts().totalSpaces(),
                summary.counts().freeCount(),
                summary.counts().occupiedCount(),
                summary.counts().unknownCount()
        );
    }
}

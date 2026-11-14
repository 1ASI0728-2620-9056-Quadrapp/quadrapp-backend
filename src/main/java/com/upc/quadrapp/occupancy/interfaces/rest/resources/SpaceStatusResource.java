package com.upc.quadrapp.occupancy.interfaces.rest.resources;

import com.upc.quadrapp.occupancy.domain.model.aggregates.SpaceOccupancy;

import java.time.Instant;

/**
 * State of one space (FREE, OCCUPIED or UNKNOWN with its reason), placed by zone and level so the
 * app and the console can overlay it on the layout.
 */
public record SpaceStatusResource(
        Long spaceId,
        Long zoneId,
        Long levelId,
        String status,
        String unknownReason,
        Instant statusSince,
        Instant lastDetectedAt
) {
    public static SpaceStatusResource from(SpaceOccupancy space) {
        return new SpaceStatusResource(
                space.getSpaceId(),
                space.getZoneId(),
                space.getLevelId(),
                space.getStatus().name(),
                space.getUnknownReason() == null ? null : space.getUnknownReason().name(),
                space.getStatusSince(),
                space.getLastDetectedAt()
        );
    }
}

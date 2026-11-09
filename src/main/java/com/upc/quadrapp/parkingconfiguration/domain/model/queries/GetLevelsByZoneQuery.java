package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

public record GetLevelsByZoneQuery(Long zoneId) {
    public GetLevelsByZoneQuery {
        if (zoneId == null || zoneId <= 0) {
            throw new IllegalArgumentException(
                    "zoneId must be positive."
            );
        }
    }
}

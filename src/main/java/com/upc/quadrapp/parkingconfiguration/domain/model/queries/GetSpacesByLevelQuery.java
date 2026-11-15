package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

public record GetSpacesByLevelQuery(Long levelId) {
    public GetSpacesByLevelQuery {
        if (levelId == null || levelId <= 0) {
            throw new IllegalArgumentException(
                    "levelId must be positive."
            );
        }
    }
}

package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

public record GetParkingLotsByInstitutionQuery(Long institutionId) {
    public GetParkingLotsByInstitutionQuery {
        if (institutionId == null || institutionId <= 0) {
            throw new IllegalArgumentException(
                    "institutionId must be positive."
            );
        }
    }
}

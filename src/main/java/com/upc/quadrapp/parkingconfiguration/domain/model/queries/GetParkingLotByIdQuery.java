package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

public record GetParkingLotByIdQuery(Long parkingLotId) {
    public GetParkingLotByIdQuery {
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException(
                    "parkingLotId must be positive."
            );
        }
    }
}

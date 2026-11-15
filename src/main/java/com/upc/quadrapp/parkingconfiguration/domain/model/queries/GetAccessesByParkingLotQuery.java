package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

public record GetAccessesByParkingLotQuery(Long parkingLotId) {
    public GetAccessesByParkingLotQuery {
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException(
                    "parkingLotId must be positive."
            );
        }
    }
}

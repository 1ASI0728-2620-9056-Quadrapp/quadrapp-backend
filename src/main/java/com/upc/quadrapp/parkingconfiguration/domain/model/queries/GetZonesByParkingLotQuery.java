package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

public record GetZonesByParkingLotQuery(Long parkingLotId) {
    public GetZonesByParkingLotQuery {
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException(
                    "parkingLotId must be positive."
            );
        }
    }
}

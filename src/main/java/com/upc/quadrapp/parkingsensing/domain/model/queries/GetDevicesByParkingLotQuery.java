package com.upc.quadrapp.parkingsensing.domain.model.queries;

public record GetDevicesByParkingLotQuery(Long institutionId, Long parkingLotId) {
    public GetDevicesByParkingLotQuery {
        if (institutionId == null || institutionId <= 0) throw new IllegalArgumentException("institutionId must be positive.");
        if (parkingLotId == null || parkingLotId <= 0) throw new IllegalArgumentException("parkingLotId must be positive.");
    }
}

package com.upc.quadrapp.parkingsensing.domain.model.queries;

public record GetDeviceIncidentsQuery(Long institutionId, Long parkingLotId, boolean onlyOpen) {
    public GetDeviceIncidentsQuery {
        if (institutionId == null || institutionId <= 0) throw new IllegalArgumentException("institutionId must be positive.");
        if (parkingLotId != null && parkingLotId <= 0) throw new IllegalArgumentException("parkingLotId must be positive.");
    }
}

package com.upc.quadrapp.parkingsensing.domain.model.queries;

public record GetDeviceByIdQuery(Long deviceId) {
    public GetDeviceByIdQuery {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId must be positive.");
    }
}

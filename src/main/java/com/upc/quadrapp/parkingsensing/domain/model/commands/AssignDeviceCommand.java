package com.upc.quadrapp.parkingsensing.domain.model.commands;

public record AssignDeviceCommand(Long deviceId, Long spaceId, Long accessId) {
    public AssignDeviceCommand {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId must be positive.");
        if ((spaceId == null) == (accessId == null)) {
            throw new IllegalArgumentException("Specify exactly one target: spaceId or accessId.");
        }
        if (spaceId != null && spaceId <= 0) throw new IllegalArgumentException("spaceId must be positive.");
        if (accessId != null && accessId <= 0) throw new IllegalArgumentException("accessId must be positive.");
    }
}

package com.upc.quadrapp.parkingsensing.domain.model.commands;

public record DecommissionDeviceCommand(Long deviceId) {
    public DecommissionDeviceCommand {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId must be positive.");
    }
}

package com.upc.quadrapp.parkingsensing.domain.model.queries;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.HardwareIdentifier;

public record GetDeviceByHardwareIdQuery(String hardwareId) {
    public GetDeviceByHardwareIdQuery {
        hardwareId = new HardwareIdentifier(hardwareId).value();
    }
}

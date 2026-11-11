package com.upc.quadrapp.parkingsensing.domain.services;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.commands.*;

public interface DeviceCommandService {
    Device handle(RegisterDeviceCommand command);
    Device handle(AssignDeviceCommand command);
    Device handle(DecommissionDeviceCommand command);
}

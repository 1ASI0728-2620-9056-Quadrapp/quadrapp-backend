package com.upc.quadrapp.parkingsensing.interfaces.rest.transform;

import com.upc.quadrapp.parkingsensing.domain.model.commands.AssignDeviceCommand;
import com.upc.quadrapp.parkingsensing.domain.model.commands.DecommissionDeviceCommand;
import com.upc.quadrapp.parkingsensing.domain.model.commands.RegisterDeviceCommand;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.AssignDeviceResource;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.RegisterDeviceResource;

public final class DeviceCommandFromResourceAssembler {

    private DeviceCommandFromResourceAssembler() {}

    public static RegisterDeviceCommand toCommand(
            Long institutionId,
            RegisterDeviceResource resource
    ) {
        return new RegisterDeviceCommand(
                institutionId,
                resource.parkingLotId(),
                resource.hardwareId(),
                resource.type(),
                resource.reportIntervalSeconds()
        );
    }

    public static AssignDeviceCommand toCommand(
            Long deviceId,
            AssignDeviceResource resource
    ) {
        return new AssignDeviceCommand(
                deviceId,
                resource.spaceId(),
                resource.accessId()
        );
    }

    public static DecommissionDeviceCommand toDecommissionCommand(Long deviceId) {
        return new DecommissionDeviceCommand(deviceId);
    }
}
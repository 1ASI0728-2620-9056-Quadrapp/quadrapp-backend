package com.upc.quadrapp.parkingsensing.interfaces.rest.transform;

import com.upc.quadrapp.parkingsensing.domain.model.commands.*;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.*;

public final class DeviceCommandFromResourceAssembler {

    private DeviceCommandFromResourceAssembler() {}

    public static RegisterDeviceCommand toCommand(
            Long institutionId,
            Long actorId,
            RegisterDeviceResource resource
    ) {
        return new RegisterDeviceCommand(
                institutionId,
                resource.parkingLotId(),
                resource.hardwareId(),
                resource.type(),
                resource.reportIntervalSeconds(),
                resource.gatewayId(),
                resource.directional(),
                resource.firmwareVersion(),
                actorId
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

    public static DecommissionDeviceCommand toDecommissionCommand(
            Long deviceId
    ) {
        return new DecommissionDeviceCommand(deviceId);
    }
}

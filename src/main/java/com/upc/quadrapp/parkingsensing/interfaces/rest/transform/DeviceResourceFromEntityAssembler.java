package com.upc.quadrapp.parkingsensing.interfaces.rest.transform;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.DeviceResource;

public final class DeviceResourceFromEntityAssembler {

    private DeviceResourceFromEntityAssembler() {}

    public static DeviceResource toResourceFromEntity(Device device) {
        return new DeviceResource(
                device.getId(),
                device.getInstitutionId(),
                device.getParkingLotId(),
                device.getHardwareId(),
                device.getType(),
                device.getReportIntervalSeconds(),
                device.getStatus(),
                device.getSpaceId(),
                device.getAccessId(),
                device.getLastSeenAt(),
                device.getRegisteredAt()
        );
    }
}
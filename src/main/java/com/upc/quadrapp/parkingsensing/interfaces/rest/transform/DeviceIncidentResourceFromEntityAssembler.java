package com.upc.quadrapp.parkingsensing.interfaces.rest.transform;

import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.DeviceIncidentResource;

public final class DeviceIncidentResourceFromEntityAssembler {

    private DeviceIncidentResourceFromEntityAssembler() {}

    public static DeviceIncidentResource toResourceFromEntity(DeviceIncident incident) {
        return new DeviceIncidentResource(
                incident.getId(),
                incident.getInstitutionId(),
                incident.getParkingLotId(),
                incident.getDeviceId(),
                incident.getHardwareId(),
                incident.getType(),
                incident.getDetails(),
                incident.getOccurredAt(),
                incident.getResolvedAt(),
                incident.isOpen()
        );
    }
}
package com.upc.quadrapp.parkingsensing.interfaces.rest.transform;

import com.upc.quadrapp.parkingsensing.domain.model.commands.ReportDeviceIncidentCommand;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ResolveDeviceIncidentCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;

import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.ReportDeviceIncidentResource;

import java.time.Instant;

public final class DeviceIncidentCommandFromResourceAssembler {

    private DeviceIncidentCommandFromResourceAssembler() {}

    public static ReportDeviceIncidentCommand toCommand(
            Long institutionId,
            Long actorId,
            ReportDeviceIncidentResource resource,
            Instant now
    ) {
        return new ReportDeviceIncidentCommand(
                institutionId,
                resource.parkingLotId(),
                resource.deviceId(),
                resource.hardwareId(),
                IncidentType.USER_REPORT,
                resource.category(),
                actorId,
                resource.details(),
                now
        );
    }

    public static ResolveDeviceIncidentCommand toResolveCommand(
            Long incidentId,
            Long actorId,
            Instant resolvedAt
    ) {
        return new ResolveDeviceIncidentCommand(
                incidentId,
                actorId,
                resolvedAt
        );
    }
}

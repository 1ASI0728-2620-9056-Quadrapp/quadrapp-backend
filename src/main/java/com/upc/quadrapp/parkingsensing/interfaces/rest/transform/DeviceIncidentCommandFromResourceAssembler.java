package com.upc.quadrapp.parkingsensing.interfaces.rest.transform;

import com.upc.quadrapp.parkingsensing.domain.model.commands.ReportDeviceIncidentCommand;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ResolveDeviceIncidentCommand;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.ReportDeviceIncidentResource;
import java.time.Instant;

public final class DeviceIncidentCommandFromResourceAssembler {

    private DeviceIncidentCommandFromResourceAssembler() {}

    public static ReportDeviceIncidentCommand toCommand(
            Long institutionId,
            ReportDeviceIncidentResource resource
    ) {
        return new ReportDeviceIncidentCommand(
                institutionId,
                resource.parkingLotId(),
                resource.deviceId(),
                resource.hardwareId(),
                resource.type(),
                resource.details(),
                resource.occurredAt()
        );
    }

    public static ResolveDeviceIncidentCommand toResolveCommand(
            Long incidentId,
            Long authenticatedUserId,
            Instant resolvedAt
    ) {
        return new ResolveDeviceIncidentCommand(
                incidentId,
                authenticatedUserId,
                resolvedAt
        );
    }
}
package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import java.time.Instant;

public record DeviceIncidentResource(
        Long id,
        Long institutionId,
        Long parkingLotId,
        Long deviceId,
        String hardwareId,
        IncidentType type,
        String details,
        Instant occurredAt,
        Instant resolvedAt,
        boolean open
) {}
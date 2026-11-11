package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;
import java.time.Instant;

public record DeviceResource(
        Long id,
        Long institutionId,
        Long parkingLotId,
        String hardwareId,
        DeviceType type,
        int reportIntervalSeconds,
        DeviceStatus status,
        Long spaceId,
        Long accessId,
        Instant lastSeenAt,
        Instant registeredAt
) {}
package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;

import java.time.Instant;

public record DeviceResource(Long id, Long institutionId, Long parkingLotId, String hardwareId,
        DeviceType type, int reportIntervalSeconds, DeviceStatus status, Long gatewayId,
        Long spaceId, Long accessId, Long replacedBy, Long registeredBy, Boolean directional,
        Integer batteryPct, String firmwareVersion, Instant lastSeenAt, Instant decommissionedAt, Instant registeredAt, Instant updatedAt) {}

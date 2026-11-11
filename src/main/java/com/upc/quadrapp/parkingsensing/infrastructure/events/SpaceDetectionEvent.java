package com.upc.quadrapp.parkingsensing.infrastructure.events;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;

import java.time.Instant;

public record SpaceDetectionEvent(
        Long institutionId,
        Long parkingLotId,
        Long spaceId,
        Long deviceId,
        String eventId,
        SensorState state,
        Instant readingAt
) {}

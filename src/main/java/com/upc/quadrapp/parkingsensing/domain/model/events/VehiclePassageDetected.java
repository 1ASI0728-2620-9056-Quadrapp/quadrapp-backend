package com.upc.quadrapp.parkingsensing.domain.model.events;

import java.time.Instant;

/**
 * A passage sensor detected a vehicle entering or leaving through an access point.
 */
public record VehiclePassageDetected(
        String eventId,
        Long deviceId,
        Long tenantId,
        Long parkingLotId,
        Long accessPointId,
        Direction direction,
        Instant detectedAt
) {
    public enum Direction {
        IN,
        OUT
    }
}

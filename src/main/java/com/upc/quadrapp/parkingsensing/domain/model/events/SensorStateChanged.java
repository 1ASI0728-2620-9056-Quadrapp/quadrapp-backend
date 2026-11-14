package com.upc.quadrapp.parkingsensing.domain.model.events;

import java.time.Instant;

/**
 * A space sensor confirmed a new state after the minimum detection time.
 * Keeps the {@code eventId} of the reading that confirmed it, so consumers can drop duplicates.
 */
public record SensorStateChanged(
        String eventId,
        Long deviceId,
        Long tenantId,
        Long parkingLotId,
        Long spaceId,
        State state,
        Instant detectedAt
) {
    public enum State {
        FREE,
        OCCUPIED
    }
}

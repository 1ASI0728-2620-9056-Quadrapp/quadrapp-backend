package com.upc.quadrapp.occupancy.domain.model.events;

import java.time.Instant;

/**
 * The space has no reliable information. Notifications groups these by zone for the stale-data
 * alert; reasons that do not mean lost data (NO_SENSOR, NOT_YET_REPORTED) raise no alert.
 */
public record SpaceMarkedUnknown(
        Long tenantId,
        Long spaceId,
        Long parkingLotId,
        Long zoneId,
        Reason reason,
        Instant changedAt
) {
    public enum Reason {
        NOT_YET_REPORTED,
        NO_SENSOR,
        SENSOR_FAULT,
        GATEWAY_OFFLINE,
        AWAITING_STABLE_DETECTION
    }
}

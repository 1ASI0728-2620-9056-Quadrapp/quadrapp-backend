package com.upc.quadrapp.parkingconfiguration.domain.model.events;

import java.time.Instant;

/**
 * A campus event was cancelled. {@code parkingLotId} is null when the event affected every
 * parking lot of the institution.
 */
public record CampusEventCancelled(
        Long campusEventId,
        Long tenantId,
        Long parkingLotId,
        Instant startAt,
        Instant endAt
) {
}

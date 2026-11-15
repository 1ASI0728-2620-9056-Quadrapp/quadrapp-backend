package com.upc.quadrapp.parkingconfiguration.domain.model.events;

import java.time.Instant;

/**
 * A campus event was registered. {@code parkingLotId} is null when the event affects every
 * parking lot of the institution. {@code impact} is LOW, MEDIUM or HIGH.
 */
public record CampusEventProvided(
        Long campusEventId,
        Long tenantId,
        Long parkingLotId,
        Instant startAt,
        Instant endAt,
        String impact
) {
}

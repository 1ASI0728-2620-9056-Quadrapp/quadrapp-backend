package com.upc.quadrapp.occupancy.domain.model.events;

import java.time.Instant;

/** A confirmed detection turned the space OCCUPIED. */
public record SpaceMarkedOccupied(Long spaceId, Long parkingLotId, Long zoneId, Instant changedAt) {
}

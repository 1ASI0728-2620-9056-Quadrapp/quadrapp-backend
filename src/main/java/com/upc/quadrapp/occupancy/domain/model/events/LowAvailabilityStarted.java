package com.upc.quadrapp.occupancy.domain.model.events;

import java.time.Instant;

/** The free spaces of the parking lot fell below its low-availability threshold. */
public record LowAvailabilityStarted(Long tenantId, Long parkingLotId, long freeSpaces, long capacity, Instant at) {
}

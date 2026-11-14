package com.upc.quadrapp.occupancy.domain.model.events;

import java.time.Instant;

/** The free spaces of the parking lot rose above its low-availability threshold plus 5 points. */
public record LowAvailabilityEnded(Long tenantId, Long parkingLotId, long freeSpaces, long capacity, Instant at) {
}

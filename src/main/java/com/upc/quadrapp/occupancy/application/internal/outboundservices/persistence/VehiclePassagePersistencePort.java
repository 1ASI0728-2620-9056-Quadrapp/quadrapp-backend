package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import java.time.Instant;

public interface VehiclePassagePersistencePort {

    /** Records the passage once per event ID; returns false if it was already recorded. */
    boolean appendIfAbsent(String eventId, Long tenantId, Long parkingLotId, Long accessPointId, boolean entering,
                           Instant passedAt, Instant recordedAt);

    int deletePassedBefore(Instant cutoff);
}

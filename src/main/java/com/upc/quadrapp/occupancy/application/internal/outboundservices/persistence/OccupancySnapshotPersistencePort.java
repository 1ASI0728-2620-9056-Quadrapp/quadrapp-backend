package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;

import java.time.Instant;
import java.util.List;

public interface OccupancySnapshotPersistencePort {

    OccupancySnapshot save(OccupancySnapshot snapshot);

    List<OccupancySnapshot> findByZoneIdAndTakenAtBetween(
            Long zoneId,
            Instant from,
            Instant to
    );

    List<OccupancySnapshot> findByParkingLotIdAndTakenAtBetween(
            Long parkingLotId,
            Instant from,
            Instant to
    );

    /**
     * Saves one snapshot per zone of every parking lot, taken at {@code takenAt}, from the stored
     * space states in one statement. A zone that already has a snapshot at that moment keeps it.
     */
    int recordAllZones(Instant takenAt);

    int deleteTakenBefore(Instant cutoff);
}

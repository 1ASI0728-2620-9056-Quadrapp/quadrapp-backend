package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;

import java.time.Instant;
import java.util.List;

public interface OccupancySnapshotPersistencePort {

    OccupancySnapshot save(OccupancySnapshot snapshot);

    List<OccupancySnapshot> findByZoneId(Long zoneId);

    List<OccupancySnapshot> findByZoneIdAndTakenAtBetween(
            Long zoneId,
            Instant from,
            Instant to
    );
}

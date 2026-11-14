package com.upc.quadrapp.occupancy.domain.services;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.aggregates.SpaceOccupancy;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;

import java.time.Instant;
import java.util.List;

public interface OccupancyQueryService {

    OccupancySummary getByParkingLot(Long parkingLotId);

    OccupancySummary getByZone(Long zoneId);

    OccupancySummary getByLevel(Long levelId);

    List<OccupancySnapshot> getSnapshots(Long zoneId, Instant from, Instant to);

    List<OccupancySnapshot> getSnapshotsByParkingLot(
            Long parkingLotId,
            Instant from,
            Instant to
    );

    OccupancySummary getByParkingLotForInternalUse(Long parkingLotId);

    /** State of each active space of the lot, to overlay on its layout. */
    List<SpaceOccupancy> getSpaceStatuses(Long parkingLotId);

    /** Whether the authenticated user may see the parking lot (used to authorize subscriptions). */
    boolean isReadableParkingLot(Long parkingLotId);
}

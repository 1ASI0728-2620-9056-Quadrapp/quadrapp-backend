package com.upc.quadrapp.occupancy.domain.services;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;

import java.time.Instant;
import java.util.List;

public interface OccupancyQueryService {

    OccupancySummary getByParkingLot(Long parkingLotId);

    OccupancySummary getByZone(Long zoneId);

    OccupancySummary getByLevel(Long levelId);

    List<OccupancySnapshot> getSnapshots(Long zoneId, Instant from, Instant to);
}

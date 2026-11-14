package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.aggregates.SpaceOccupancy;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.ZoneCount;

import java.util.List;
import java.util.Optional;

public interface SpaceOccupancyPersistencePort {

    /** Loads the space and locks its row until the transaction ends. */
    Optional<SpaceOccupancy> findByIdForUpdate(Long spaceId);

    List<SpaceOccupancy> findByParkingLotId(Long parkingLotId);

    /** Saves the spaces and writes them to the database at once, so later counts include them. */
    void saveAll(List<SpaceOccupancy> spaces);

    /** States of the active spaces of the lot, grouped by zone (one query). */
    List<ZoneCount> countByZone(Long parkingLotId);

    OccupancyCounts countByZoneId(Long zoneId);

    OccupancyCounts countByLevelId(Long levelId);
}

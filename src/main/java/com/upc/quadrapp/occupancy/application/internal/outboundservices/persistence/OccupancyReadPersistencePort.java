package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyScopeType;

import java.util.Optional;

public interface OccupancyReadPersistencePort {

    OccupancyCounts summarize(OccupancyScopeType scopeType, Long scopeId);

    Optional<Long> findParkingLotIdForZone(Long zoneId);

    Optional<Long> findParkingLotIdForLevel(Long levelId);
}

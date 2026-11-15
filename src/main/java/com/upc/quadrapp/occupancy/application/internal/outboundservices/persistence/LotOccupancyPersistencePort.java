package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.aggregates.LotOccupancy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LotOccupancyPersistencePort {

    Optional<LotOccupancy> findById(Long parkingLotId);

    /** Loads the parking lot and locks its row until the transaction ends. */
    Optional<LotOccupancy> findByIdForUpdate(Long parkingLotId);

    /**
     * Creates the empty parking lot unless it exists. Two replicas creating it at once do not
     * fail: the second insert waits for the first and then does nothing.
     */
    void createIfAbsent(Long parkingLotId, Long tenantId, int lowAvailabilityPct, Instant now);

    /** Saves the lot and returns it with its new version. */
    LotOccupancy save(LotOccupancy lot);

    List<Long> findAllIds();

    /** Replaces the accesses of the parking lot with those of the current layout. */
    void replaceAccessPoints(Long parkingLotId, List<Long> accessPointIds, List<Long> flowCountingIds);

    boolean hasAccessPoint(Long parkingLotId, Long accessPointId);
}

package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancyReadPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyScopeType;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.OccupancySnapshotJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public class OccupancyReadPersistenceAdapter implements OccupancyReadPersistencePort {

    private final OccupancySnapshotJpaRepository repository;

    public OccupancyReadPersistenceAdapter(OccupancySnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OccupancyCounts summarize(OccupancyScopeType scopeType, Long scopeId) {
        var result = repository.summarize(scopeType.name(), scopeId);
        return new OccupancyCounts(
                result.getTotalSpaces(),
                result.getFreeCount(),
                result.getOccupiedCount(),
                result.getUnknownCount()
        );
    }

    @Override
    public Optional<Long> findParkingLotIdForZone(Long zoneId) {
        return repository.findParkingLotIdForZone(zoneId);
    }

    @Override
    public Optional<Long> findParkingLotIdForLevel(Long levelId) {
        return repository.findParkingLotIdForLevel(levelId);
    }

    @Override
    public List<Long> findZoneIdsForParkingLot(Long parkingLotId) {
        return repository.findZoneIdsForParkingLot(parkingLotId);
    }
}

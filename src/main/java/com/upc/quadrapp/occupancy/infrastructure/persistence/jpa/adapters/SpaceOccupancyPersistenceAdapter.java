package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SpaceOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.SpaceOccupancy;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.ZoneCount;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.SpaceOccupancyJpaEntity;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.SpaceOccupancyJpaRepository;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.SpaceOccupancyJpaRepository.CountsProjection;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SpaceOccupancyPersistenceAdapter implements SpaceOccupancyPersistencePort {

    private final SpaceOccupancyJpaRepository repository;

    public SpaceOccupancyPersistenceAdapter(SpaceOccupancyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SpaceOccupancy> findByIdForUpdate(Long spaceId) {
        return repository.findByIdForUpdate(spaceId).map(SpaceOccupancyPersistenceAdapter::toDomain);
    }

    @Override
    public List<SpaceOccupancy> findByParkingLotId(Long parkingLotId) {
        return repository.findByParkingLotId(parkingLotId).stream()
                .map(SpaceOccupancyPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<SpaceOccupancy> spaces) {
        if (spaces.isEmpty()) {
            return;
        }
        repository.saveAll(spaces.stream().map(SpaceOccupancyPersistenceAdapter::toJpa).toList());
        repository.flush();
    }

    @Override
    public List<ZoneCount> countByZone(Long parkingLotId) {
        return repository.countByZone(parkingLotId).stream()
                .map(row -> new ZoneCount(row.getZoneId(), counts(row)))
                .toList();
    }

    @Override
    public OccupancyCounts countByZoneId(Long zoneId) {
        return counts(repository.countByZoneId(zoneId));
    }

    @Override
    public OccupancyCounts countByLevelId(Long levelId) {
        return counts(repository.countByLevelId(levelId));
    }

    private static OccupancyCounts counts(CountsProjection row) {
        return new OccupancyCounts(
                row.getTotalSpaces(),
                row.getFreeCount(),
                row.getOccupiedCount(),
                row.getUnknownCount()
        );
    }

    private static SpaceOccupancy toDomain(SpaceOccupancyJpaEntity entity) {
        return SpaceOccupancy.restore(
                entity.getSpaceId(),
                entity.getTenantId(),
                entity.getParkingLotId(),
                entity.getZoneId(),
                entity.getLevelId(),
                entity.isActive(),
                entity.getStatus(),
                entity.getUnknownReason(),
                entity.getStatusSince(),
                entity.getLastDetectedAt(),
                entity.getLastEventId(),
                entity.getVersion()
        );
    }

    private static SpaceOccupancyJpaEntity toJpa(SpaceOccupancy space) {
        SpaceOccupancyJpaEntity entity = new SpaceOccupancyJpaEntity();
        entity.setSpaceId(space.getSpaceId());
        entity.setTenantId(space.getTenantId());
        entity.setParkingLotId(space.getParkingLotId());
        entity.setZoneId(space.getZoneId());
        entity.setLevelId(space.getLevelId());
        entity.setActive(space.isActive());
        entity.setStatus(space.getStatus());
        entity.setUnknownReason(space.getUnknownReason());
        entity.setStatusSince(space.getStatusSince());
        entity.setLastDetectedAt(space.getLastDetectedAt());
        entity.setLastEventId(space.getLastEventId());
        entity.setVersion(space.getVersion());
        return entity;
    }
}

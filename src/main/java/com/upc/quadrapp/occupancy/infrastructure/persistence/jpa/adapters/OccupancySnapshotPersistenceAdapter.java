package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancySnapshotPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.OccupancySnapshotJpaEntity;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.OccupancySnapshotJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class OccupancySnapshotPersistenceAdapter implements OccupancySnapshotPersistencePort {

    private final OccupancySnapshotJpaRepository repository;

    public OccupancySnapshotPersistenceAdapter(OccupancySnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OccupancySnapshot save(OccupancySnapshot snapshot) {
        OccupancySnapshotJpaEntity entity = new OccupancySnapshotJpaEntity();
        entity.setZoneId(snapshot.zoneId());
        entity.setTakenAt(snapshot.takenAt());
        entity.setTotalSpaces((short) snapshot.counts().totalSpaces());
        entity.setFreeCount((short) snapshot.counts().freeCount());
        entity.setOccupiedCount((short) snapshot.counts().occupiedCount());
        entity.setUnknownCount((short) snapshot.counts().unknownCount());
        return toDomain(repository.save(entity));
    }

    @Override
    public List<OccupancySnapshot> findByZoneId(Long zoneId) {
        return repository.findByZoneIdOrderByTakenAtDesc(zoneId)
                .stream()
                .map(OccupancySnapshotPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<OccupancySnapshot> findByZoneIdAndTakenAtBetween(
            Long zoneId,
            Instant from,
            Instant to
    ) {
        return repository
                .findByZoneIdAndTakenAtGreaterThanEqualAndTakenAtLessThanEqualOrderByTakenAtDesc(
                        zoneId,
                        from,
                        to
                )
                .stream()
                .map(OccupancySnapshotPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<OccupancySnapshot> findByParkingLotId(Long parkingLotId) {
        return repository.findByParkingLotId(parkingLotId)
                .stream()
                .map(OccupancySnapshotPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<OccupancySnapshot> findByParkingLotIdAndTakenAtBetween(
            Long parkingLotId,
            Instant from,
            Instant to
    ) {
        return repository.findByParkingLotIdAndTakenAtBetween(parkingLotId, from, to)
                .stream()
                .map(OccupancySnapshotPersistenceAdapter::toDomain)
                .toList();
    }

    private static OccupancySnapshot toDomain(OccupancySnapshotJpaEntity entity) {
        return new OccupancySnapshot(
                entity.getId(),
                entity.getZoneId(),
                entity.getTakenAt(),
                new OccupancyCounts(
                        entity.getTotalSpaces(),
                        entity.getFreeCount(),
                        entity.getOccupiedCount(),
                        entity.getUnknownCount()
                )
        );
    }
}

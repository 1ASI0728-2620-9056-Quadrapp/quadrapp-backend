package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.LotOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.LotOccupancy;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.LotOccupancyJpaEntity;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.LotOccupancyJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class LotOccupancyPersistenceAdapter implements LotOccupancyPersistencePort {

    private final LotOccupancyJpaRepository repository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    public LotOccupancyPersistenceAdapter(
            LotOccupancyJpaRepository repository,
            EntityManager entityManager,
            JdbcTemplate jdbc
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    /**
     * Reads without keeping the entity in the persistence context, so a later locked read in the
     * same transaction loads the current row instead of this copy.
     */
    @Override
    public Optional<LotOccupancy> findById(Long parkingLotId) {
        return repository.findById(parkingLotId).map(entity -> {
            entityManager.detach(entity);
            return toDomain(entity);
        });
    }

    @Override
    public Optional<LotOccupancy> findByIdForUpdate(Long parkingLotId) {
        return repository.findByIdForUpdate(parkingLotId).map(LotOccupancyPersistenceAdapter::toDomain);
    }

    @Override
    public void createIfAbsent(Long parkingLotId, Long tenantId, int lowAvailabilityPct, Instant now) {
        repository.createIfAbsent(parkingLotId, tenantId, lowAvailabilityPct, now);
    }

    @Override
    public LotOccupancy save(LotOccupancy lot) {
        return toDomain(repository.saveAndFlush(toJpa(lot)));
    }

    @Override
    public List<Long> findAllIds() {
        return repository.findAllIds();
    }

    @Override
    public void replaceAccessPoints(Long parkingLotId, List<Long> accessPointIds, List<Long> flowCountingIds) {
        jdbc.update("DELETE FROM occupancy.lot_access_points WHERE parking_lot_id = ?", parkingLotId);
        jdbc.batchUpdate("""
                        INSERT INTO occupancy.lot_access_points (access_point_id, parking_lot_id, flow_counting)
                        VALUES (?, ?, ?)
                        ON CONFLICT (access_point_id)
                        DO UPDATE SET parking_lot_id = EXCLUDED.parking_lot_id, flow_counting = EXCLUDED.flow_counting
                        """,
                accessPointIds.stream()
                        .map(id -> new Object[]{id, parkingLotId, flowCountingIds.contains(id)})
                        .toList());
    }

    @Override
    public boolean hasAccessPoint(Long parkingLotId, Long accessPointId) {
        Integer found = jdbc.queryForObject("""
                SELECT COUNT(*) FROM occupancy.lot_access_points
                WHERE parking_lot_id = ? AND access_point_id = ?
                """, Integer.class, parkingLotId, accessPointId);
        return found != null && found > 0;
    }

    private static LotOccupancy toDomain(LotOccupancyJpaEntity entity) {
        return LotOccupancy.restore(
                entity.getParkingLotId(),
                entity.getTenantId(),
                new OccupancyCounts(
                        entity.getTotalSpaces(),
                        entity.getFreeCount(),
                        entity.getOccupiedCount(),
                        entity.getUnknownCount()
                ),
                entity.getLowAvailabilityPct(),
                entity.isLowAvailability(),
                entity.getConsolidatedAt(),
                entity.getLayoutSyncedAt(),
                entity.getVersion()
        );
    }

    private static LotOccupancyJpaEntity toJpa(LotOccupancy lot) {
        LotOccupancyJpaEntity entity = new LotOccupancyJpaEntity();
        entity.setParkingLotId(lot.getParkingLotId());
        entity.setTenantId(lot.getTenantId());
        entity.setTotalSpaces(Math.toIntExact(lot.getCounts().totalSpaces()));
        entity.setFreeCount(Math.toIntExact(lot.getCounts().freeCount()));
        entity.setOccupiedCount(Math.toIntExact(lot.getCounts().occupiedCount()));
        entity.setUnknownCount(Math.toIntExact(lot.getCounts().unknownCount()));
        entity.setLowAvailabilityPct((short) lot.getLowAvailabilityPct());
        entity.setLowAvailability(lot.isLowAvailability());
        entity.setConsolidatedAt(lot.getConsolidatedAt());
        entity.setLayoutSyncedAt(lot.getLayoutSyncedAt());
        entity.setVersion(lot.getVersion());
        return entity;
    }
}

package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.OccupancySnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OccupancySnapshotJpaRepository
        extends JpaRepository<OccupancySnapshotJpaEntity, Long> {

    List<OccupancySnapshotJpaEntity> findByZoneIdOrderByTakenAtDesc(Long zoneId);

    List<OccupancySnapshotJpaEntity> findByZoneIdAndTakenAtGreaterThanEqualAndTakenAtLessThanEqualOrderByTakenAtDesc(
            Long zoneId,
            java.time.Instant from,
            java.time.Instant to
    );

    @Query(value = """
            SELECT snapshot.*
            FROM occupancy.occupancy_snapshots snapshot
            JOIN configuration.zones zone ON zone.id = snapshot.zone_id
            WHERE zone.lot_id = :parkingLotId
            ORDER BY snapshot.taken_at ASC, snapshot.zone_id ASC
            """, nativeQuery = true)
    List<OccupancySnapshotJpaEntity> findByParkingLotId(
            @Param("parkingLotId") Long parkingLotId
    );

    @Query(value = """
            SELECT snapshot.*
            FROM occupancy.occupancy_snapshots snapshot
            JOIN configuration.zones zone ON zone.id = snapshot.zone_id
            WHERE zone.lot_id = :parkingLotId
              AND snapshot.taken_at >= :from
              AND snapshot.taken_at <= :to
            ORDER BY snapshot.taken_at ASC, snapshot.zone_id ASC
            """, nativeQuery = true)
    List<OccupancySnapshotJpaEntity> findByParkingLotIdAndTakenAtBetween(
            @Param("parkingLotId") Long parkingLotId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to
    );

    @Query(value = """
            SELECT COUNT(*) AS "totalSpaces",
                   COUNT(*) FILTER (WHERE s.status = 'FREE') AS "freeCount",
                   COUNT(*) FILTER (WHERE s.status = 'OCCUPIED') AS "occupiedCount",
                   COUNT(*) FILTER (WHERE s.status = 'UNKNOWN') AS "unknownCount"
            FROM configuration.spaces s
            JOIN configuration.levels l ON l.id = s.level_id
            JOIN configuration.zones z ON z.id = l.zone_id
            WHERE s.active = TRUE
              AND (
                    (:scopeType = 'PARKING_LOT' AND z.lot_id = :scopeId)
                 OR (:scopeType = 'ZONE' AND z.id = :scopeId)
                 OR (:scopeType = 'LEVEL' AND l.id = :scopeId)
              )
            """, nativeQuery = true)
    OccupancyCountsProjection summarize(
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId
    );

    @Query(value = """
            SELECT z.lot_id
            FROM configuration.zones z
            WHERE z.id = :zoneId
            """, nativeQuery = true)
    Optional<Long> findParkingLotIdForZone(@Param("zoneId") Long zoneId);

    @Query(value = """
            SELECT z.lot_id
            FROM configuration.levels l
            JOIN configuration.zones z ON z.id = l.zone_id
            WHERE l.id = :levelId
            """, nativeQuery = true)
    Optional<Long> findParkingLotIdForLevel(@Param("levelId") Long levelId);

    @Query(value = """
            SELECT z.id
            FROM configuration.zones z
            WHERE z.lot_id = :parkingLotId
            ORDER BY z.id
            """, nativeQuery = true)
    List<Long> findZoneIdsForParkingLot(@Param("parkingLotId") Long parkingLotId);

    interface OccupancyCountsProjection {
        Long getTotalSpaces();

        Long getFreeCount();

        Long getOccupiedCount();

        Long getUnknownCount();
    }
}

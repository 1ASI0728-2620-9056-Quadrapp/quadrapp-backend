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
            SELECT COUNT(*) AS "totalSpaces",
                   COUNT(*) FILTER (WHERE s.status = 'FREE') AS "freeCount",
                   COUNT(*) FILTER (WHERE s.status = 'OCCUPIED') AS "occupiedCount",
                   COUNT(*) FILTER (WHERE s.status = 'UNKNOWN') AS "unknownCount"
            FROM public.spaces s
            JOIN public.levels l ON l.id = s.level_id
            JOIN public.zones z ON z.id = l.zone_id
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
            FROM public.zones z
            WHERE z.id = :zoneId
            """, nativeQuery = true)
    Optional<Long> findParkingLotIdForZone(@Param("zoneId") Long zoneId);

    @Query(value = """
            SELECT z.lot_id
            FROM public.levels l
            JOIN public.zones z ON z.id = l.zone_id
            WHERE l.id = :levelId
            """, nativeQuery = true)
    Optional<Long> findParkingLotIdForLevel(@Param("levelId") Long levelId);

    interface OccupancyCountsProjection {
        Long getTotalSpaces();

        Long getFreeCount();

        Long getOccupiedCount();

        Long getUnknownCount();
    }
}

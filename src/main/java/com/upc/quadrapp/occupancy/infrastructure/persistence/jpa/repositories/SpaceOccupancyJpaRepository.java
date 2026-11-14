package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.SpaceOccupancyJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceOccupancyJpaRepository extends JpaRepository<SpaceOccupancyJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SpaceOccupancyJpaEntity s where s.spaceId = :spaceId")
    Optional<SpaceOccupancyJpaEntity> findByIdForUpdate(@Param("spaceId") Long spaceId);

    List<SpaceOccupancyJpaEntity> findByParkingLotId(Long parkingLotId);

    @Query(value = """
            SELECT s.zone_id AS "zoneId",
                   COUNT(*) AS "totalSpaces",
                   COUNT(*) FILTER (WHERE s.status = 'FREE') AS "freeCount",
                   COUNT(*) FILTER (WHERE s.status = 'OCCUPIED') AS "occupiedCount",
                   COUNT(*) FILTER (WHERE s.status = 'UNKNOWN') AS "unknownCount"
            FROM occupancy.space_occupancy s
            WHERE s.parking_lot_id = :parkingLotId
              AND s.active
            GROUP BY s.zone_id
            ORDER BY s.zone_id
            """, nativeQuery = true)
    List<ZoneCountsProjection> countByZone(@Param("parkingLotId") Long parkingLotId);

    @Query(value = """
            SELECT COUNT(*) AS "totalSpaces",
                   COUNT(*) FILTER (WHERE s.status = 'FREE') AS "freeCount",
                   COUNT(*) FILTER (WHERE s.status = 'OCCUPIED') AS "occupiedCount",
                   COUNT(*) FILTER (WHERE s.status = 'UNKNOWN') AS "unknownCount"
            FROM occupancy.space_occupancy s
            WHERE s.zone_id = :zoneId
              AND s.active
            """, nativeQuery = true)
    CountsProjection countByZoneId(@Param("zoneId") Long zoneId);

    @Query(value = """
            SELECT COUNT(*) AS "totalSpaces",
                   COUNT(*) FILTER (WHERE s.status = 'FREE') AS "freeCount",
                   COUNT(*) FILTER (WHERE s.status = 'OCCUPIED') AS "occupiedCount",
                   COUNT(*) FILTER (WHERE s.status = 'UNKNOWN') AS "unknownCount"
            FROM occupancy.space_occupancy s
            WHERE s.level_id = :levelId
              AND s.active
            """, nativeQuery = true)
    CountsProjection countByLevelId(@Param("levelId") Long levelId);

    interface CountsProjection {
        Long getTotalSpaces();

        Long getFreeCount();

        Long getOccupiedCount();

        Long getUnknownCount();
    }

    interface ZoneCountsProjection extends CountsProjection {
        Long getZoneId();
    }
}

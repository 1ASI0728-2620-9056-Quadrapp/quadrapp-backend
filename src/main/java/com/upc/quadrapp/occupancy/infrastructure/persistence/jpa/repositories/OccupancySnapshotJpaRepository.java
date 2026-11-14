package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.OccupancySnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OccupancySnapshotJpaRepository
        extends JpaRepository<OccupancySnapshotJpaEntity, Long> {

    List<OccupancySnapshotJpaEntity> findByZoneIdAndTakenAtGreaterThanEqualAndTakenAtLessThanEqualOrderByTakenAtDesc(
            Long zoneId,
            Instant from,
            Instant to
    );

    List<OccupancySnapshotJpaEntity> findByParkingLotIdAndTakenAtGreaterThanEqualAndTakenAtLessThanEqualOrderByTakenAtAscZoneIdAsc(
            Long parkingLotId,
            Instant from,
            Instant to
    );

    @Modifying
    @Query(value = """
            INSERT INTO occupancy.occupancy_snapshots
                (parking_lot_id, zone_id, taken_at, total_spaces, free_count, occupied_count, unknown_count)
            SELECT s.parking_lot_id,
                   s.zone_id,
                   :takenAt,
                   COUNT(*),
                   COUNT(*) FILTER (WHERE s.status = 'FREE'),
                   COUNT(*) FILTER (WHERE s.status = 'OCCUPIED'),
                   COUNT(*) FILTER (WHERE s.status = 'UNKNOWN')
            FROM occupancy.space_occupancy s
            WHERE s.active
            GROUP BY s.parking_lot_id, s.zone_id
            ON CONFLICT (zone_id, taken_at) DO NOTHING
            """, nativeQuery = true)
    int recordAllZones(@Param("takenAt") Instant takenAt);

    @Modifying
    @Query(value = "DELETE FROM occupancy.occupancy_snapshots WHERE taken_at < :cutoff", nativeQuery = true)
    int deleteTakenBefore(@Param("cutoff") Instant cutoff);
}

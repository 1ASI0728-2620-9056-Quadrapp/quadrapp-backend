package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.LotOccupancyJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LotOccupancyJpaRepository extends JpaRepository<LotOccupancyJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LotOccupancyJpaEntity l where l.parkingLotId = :parkingLotId")
    Optional<LotOccupancyJpaEntity> findByIdForUpdate(@Param("parkingLotId") Long parkingLotId);

    @Modifying
    @Query(value = """
            INSERT INTO occupancy.lot_occupancy
                (parking_lot_id, tenant_id, total_spaces, free_count, occupied_count, unknown_count,
                 low_availability_pct, low_availability, consolidated_at, layout_synced_at, version)
            VALUES (:parkingLotId, :tenantId, 0, 0, 0, 0, :lowAvailabilityPct, FALSE, NULL, :now, 0)
            ON CONFLICT (parking_lot_id) DO NOTHING
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("parkingLotId") Long parkingLotId,
            @Param("tenantId") Long tenantId,
            @Param("lowAvailabilityPct") int lowAvailabilityPct,
            @Param("now") Instant now
    );

    @Query("select l.parkingLotId from LotOccupancyJpaEntity l order by l.parkingLotId")
    List<Long> findAllIds();
}

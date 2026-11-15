package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "occupancy", name = "lot_occupancy")
@Getter
@Setter
@NoArgsConstructor
public class LotOccupancyJpaEntity {

    @Id
    @Column(name = "parking_lot_id")
    private Long parkingLotId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "total_spaces", nullable = false)
    private int totalSpaces;

    @Column(name = "free_count", nullable = false)
    private int freeCount;

    @Column(name = "occupied_count", nullable = false)
    private int occupiedCount;

    @Column(name = "unknown_count", nullable = false)
    private int unknownCount;

    @Column(name = "low_availability_pct", nullable = false)
    private short lowAvailabilityPct;

    @Column(name = "low_availability", nullable = false)
    private boolean lowAvailability;

    @Column(name = "consolidated_at", columnDefinition = "timestamptz")
    private Instant consolidatedAt;

    @Column(name = "layout_synced_at", nullable = false, columnDefinition = "timestamptz")
    private Instant layoutSyncedAt;

    /** Optimistic lock: a save based on an outdated copy fails instead of overwriting. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

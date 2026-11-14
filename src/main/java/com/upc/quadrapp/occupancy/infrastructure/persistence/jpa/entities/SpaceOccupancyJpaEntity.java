package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.UnknownReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "occupancy", name = "space_occupancy")
@Getter
@Setter
@NoArgsConstructor
public class SpaceOccupancyJpaEntity {

    @Id
    @Column(name = "space_id")
    private Long spaceId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "parking_lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SpaceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "unknown_reason", length = 30)
    private UnknownReason unknownReason;

    @Column(name = "status_since", nullable = false, columnDefinition = "timestamptz")
    private Instant statusSince;

    @Column(name = "last_detected_at", columnDefinition = "timestamptz")
    private Instant lastDetectedAt;

    @Column(name = "last_event_id", length = 100)
    private String lastEventId;

    /** Optimistic lock: a save based on an outdated copy fails instead of overwriting. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

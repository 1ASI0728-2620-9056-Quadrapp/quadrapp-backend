package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.CalendarEventType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventImpact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
@NoArgsConstructor
public class CalendarEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "lot_id")
    private Long parkingLotId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 15)
    private CalendarEventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact", nullable = false, length = 6)
    private EventImpact impact;

    @Column(name = "starts_at", nullable = false, columnDefinition = "timestamptz")
    private Instant startAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "timestamptz")
    private Instant endAt;

    @Column(name = "cancelled_at", columnDefinition = "timestamptz")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;
}

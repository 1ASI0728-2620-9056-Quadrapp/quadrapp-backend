package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.UnknownReason;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(schema = "configuration", name = "spaces", uniqueConstraints = @UniqueConstraint(name = "uk_spaces_level_label", columnNames = {"level_id", "label"}))
@Getter
@Setter
@NoArgsConstructor
public class SpaceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private SpaceType type;

    @Column(name = "pos_x", precision = 7, scale = 2)
    private BigDecimal posX;

    @Column(name = "pos_y", precision = 7, scale = 2)
    private BigDecimal posY;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SpaceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "unknown_reason", length = 25)
    private UnknownReason unknownReason;

    @Column(name = "status_since", nullable = false, columnDefinition = "timestamptz")
    private Instant statusSince;

    @Column(name = "last_event_at", columnDefinition = "timestamptz")
    private Instant lastEventAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;
}

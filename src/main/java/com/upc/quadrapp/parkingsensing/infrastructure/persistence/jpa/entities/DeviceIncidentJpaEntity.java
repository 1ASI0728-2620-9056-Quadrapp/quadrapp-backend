package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentCategory;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;

@Entity
@Table(name = "device_incidents")
@Getter
@Setter
@NoArgsConstructor
public class DeviceIncidentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Formula(
            "(select lot.institution_id from parking_lots lot where lot.id = lot_id)"
    )
    @Setter(AccessLevel.NONE)
    private Long institutionId;

    @Column(name = "lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "hardware_id", length = 80)
    private String hardwareId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 25)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private IncidentCategory category;

    @Column(name = "detail", columnDefinition = "text")
    private String details;

    @Column(
            name = "detected_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private Instant occurredAt;

    @Column(
            name = "resolved_at",
            columnDefinition = "timestamptz"
    )
    private Instant resolvedAt;
}

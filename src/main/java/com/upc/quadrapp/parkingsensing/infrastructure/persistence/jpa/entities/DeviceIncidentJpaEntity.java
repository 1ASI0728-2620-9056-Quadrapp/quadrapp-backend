package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "device_incidents")
@Getter @Setter @NoArgsConstructor
public class DeviceIncidentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "parking_lot_id")
    private Long parkingLotId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "hardware_id", nullable = false, length = 128)
    private String hardwareId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private IncidentType type;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamptz")
    private Instant occurredAt;

    @Column(name = "resolved_at", columnDefinition = "timestamptz")
    private Instant resolvedAt;
}
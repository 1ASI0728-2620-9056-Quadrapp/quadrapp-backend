package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "devices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_devices_hardware_id", columnNames = "hardware_id")
})
@Getter @Setter @NoArgsConstructor
public class DeviceJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "parking_lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "hardware_id", nullable = false, length = 128)
    private String hardwareId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private DeviceType type;

    @Column(name = "report_interval_seconds", nullable = false)
    private int reportIntervalSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private DeviceStatus status;

    @Column(name = "space_id")
    private Long spaceId;

    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "last_seen_at", columnDefinition = "timestamptz")
    private Instant lastSeenAt;

    @Column(name = "registered_at", nullable = false, columnDefinition = "timestamptz")
    private Instant registeredAt;
}

package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;

@Entity
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(name = "uk_devices_hardware_id", columnNames = "hardware_id"))
@Getter
@Setter
@NoArgsConstructor
public class DeviceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Formula("(select lot.institution_id from parking_lots lot where lot.id = lot_id)")
    @Setter(AccessLevel.NONE)
    private Long institutionId;

    @Column(name = "lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "gateway_id")
    private Long gatewayId;

    @Column(name = "space_id")
    private Long spaceId;

    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "replaced_by")
    private Long replacedBy;

    @Column(name = "registered_by")
    private Long registeredBy;

    @Column(
            name = "hardware_id",
            nullable = false,
            length = 80
    )
    private String hardwareId;

    @Column(name = "api_key_hash", length = 255)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DeviceType type;

    @Column(name = "directional")
    private Boolean directional;

    @Column(
            name = "expected_report_interval_s",
            nullable = false
    )
    private int reportIntervalSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private DeviceStatus status;

    @Column(name = "battery_pct")
    private Short batteryPct;

    @Column(name = "firmware_version", length = 30)
    private String firmwareVersion;

    @Column(
            name = "last_seen_at",
            columnDefinition = "timestamptz"
    )
    private Instant lastSeenAt;

    @Column(
            name = "decommissioned_at",
            columnDefinition = "timestamptz"
    )
    private Instant decommissionedAt;

    @Column(
            name = "created_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private Instant registeredAt;

    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private Instant updatedAt;
}

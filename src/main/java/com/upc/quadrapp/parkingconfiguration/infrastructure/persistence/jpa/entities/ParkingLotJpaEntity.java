package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(schema = "configuration", name = "parking_lots", uniqueConstraints = @UniqueConstraint(name = "uk_parking_lots_institution_name", columnNames = {"institution_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class ParkingLotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "saturation_threshold_pct", nullable = false)
    private short saturationThresholdPct;

    @Column(name = "low_battery_threshold_pct", nullable = false)
    private short lowBatteryThresholdPct;

    @Column(name = "stale_after_seconds", nullable = false)
    private int staleAfterSeconds;

    @Column(name = "flow_window_minutes", nullable = false)
    private short flowWindowMinutes;

    @Column(name = "accuracy_margin_pct", nullable = false)
    private short accuracyMarginPct;

    @Column(name = "occupied_debounce_s", nullable = false)
    private short occupiedDebounceS;

    @Column(name = "freed_debounce_s", nullable = false)
    private short freedDebounceS;

    @Column(name = "low_availability_pct", nullable = false)
    private short lowAvailabilityPct;

    @Column(name = "layout_version", nullable = false)
    private int layoutVersion;

    @Column(name = "layout_published_at", columnDefinition = "timestamptz")
    private Instant layoutPublishedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;
}

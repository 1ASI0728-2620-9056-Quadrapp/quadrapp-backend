package com.upc.quadrapp.prediction.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        schema = "prediction",
        name = "forecasts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_forecasts_lot_horizon_generated",
                columnNames = {"lot_id", "horizon_min", "generated_at"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ForecastJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "horizon_min", nullable = false)
    private short horizonMinutes;

    @Column(name = "generated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant generatedAt;

    @Column(name = "target_at", nullable = false, columnDefinition = "timestamptz")
    private Instant targetAt;

    @Column(name = "expected_occupancy_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal expectedOccupancyPct;

    @Column(name = "confidence", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 6)
    private ConfidenceLevel confidenceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 15)
    private ForecastMethod method;

    @Column(name = "model_version", nullable = false, length = 40)
    private String modelVersion;

    @Column(name = "saturation_expected_at", columnDefinition = "timestamptz")
    private Instant saturationExpectedAt;

    @Column(name = "observed_occupancy_pct", precision = 5, scale = 2)
    private BigDecimal observedOccupancyPct;

    @Column(name = "abs_error", precision = 5, scale = 2)
    private BigDecimal absoluteError;
}

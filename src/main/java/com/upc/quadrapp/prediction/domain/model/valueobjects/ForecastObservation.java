package com.upc.quadrapp.prediction.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ForecastObservation(
        Instant takenAt,
        BigDecimal occupancyPct,
        BigDecimal knownCoverage
) {
    public ForecastObservation {
        Objects.requireNonNull(takenAt);
        Objects.requireNonNull(occupancyPct);
        Objects.requireNonNull(knownCoverage);
        if (occupancyPct.compareTo(BigDecimal.ZERO) < 0
                || occupancyPct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Occupancy percentage must be between 0 and 100.");
        }
        if (knownCoverage.compareTo(BigDecimal.ZERO) < 0
                || knownCoverage.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Known coverage must be between 0 and 1.");
        }
    }
}

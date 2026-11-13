package com.upc.quadrapp.prediction.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

public record ForecastAccuracyMetrics(
        long evaluatedForecasts,
        BigDecimal meanAbsoluteErrorPct
) {
    public ForecastAccuracyMetrics {
        if (evaluatedForecasts < 0) {
            throw new IllegalArgumentException("Evaluated forecast count cannot be negative.");
        }
        if (evaluatedForecasts == 0) {
            if (meanAbsoluteErrorPct != null) {
                throw new IllegalArgumentException(
                        "Mean absolute error must be absent when no forecasts were evaluated."
                );
            }
        } else {
            Objects.requireNonNull(meanAbsoluteErrorPct);
            if (meanAbsoluteErrorPct.compareTo(BigDecimal.ZERO) < 0
                    || meanAbsoluteErrorPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(
                        "Mean absolute error must be between 0 and 100."
                );
            }
        }
    }
}

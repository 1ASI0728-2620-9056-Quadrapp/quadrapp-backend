package com.upc.quadrapp.prediction.interfaces.rest.resources;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;

import java.math.BigDecimal;

public record ForecastAccuracyMetricsResource(
        long evaluatedForecasts,
        BigDecimal meanAbsoluteErrorPct
) {
    public static ForecastAccuracyMetricsResource from(ForecastAccuracyMetrics metrics) {
        return new ForecastAccuracyMetricsResource(
                metrics.evaluatedForecasts(),
                metrics.meanAbsoluteErrorPct()
        );
    }
}

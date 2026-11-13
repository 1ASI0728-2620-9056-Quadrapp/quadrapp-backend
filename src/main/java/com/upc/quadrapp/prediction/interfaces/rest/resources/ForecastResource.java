package com.upc.quadrapp.prediction.interfaces.rest.resources;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;

import java.math.BigDecimal;
import java.time.Instant;

public record ForecastResource(
        Long id,
        Long parkingLotId,
        int horizonMinutes,
        Instant generatedAt,
        Instant targetAt,
        BigDecimal expectedOccupancyPct,
        BigDecimal confidence,
        String confidenceLevel,
        String method,
        String modelVersion,
        Instant saturationExpectedAt,
        BigDecimal observedOccupancyPct,
        BigDecimal absoluteError
) {
    public static ForecastResource from(Forecast forecast) {
        return new ForecastResource(
                forecast.id(),
                forecast.parkingLotId(),
                forecast.horizonMinutes(),
                forecast.generatedAt(),
                forecast.targetAt(),
                forecast.expectedOccupancyPct(),
                forecast.confidence(),
                forecast.confidenceLevel().name(),
                forecast.method().name(),
                forecast.modelVersion(),
                forecast.saturationExpectedAt(),
                forecast.observedOccupancyPct(),
                forecast.absoluteError()
        );
    }
}

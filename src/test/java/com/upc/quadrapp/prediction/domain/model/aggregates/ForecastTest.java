package com.upc.quadrapp.prediction.domain.model.aggregates;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForecastTest {

    @Test
    void recordsObservedOccupancyAndAbsoluteError() {
        Instant generatedAt = Instant.parse("2026-01-01T10:00:00Z");
        Forecast forecast = Forecast.create(
                1L,
                15,
                generatedAt,
                new BigDecimal("62.45"),
                new BigDecimal("0.700"),
                ConfidenceLevel.MEDIUM,
                ForecastMethod.PRIMARY_MODEL,
                "linear-trend-v1",
                null
        );

        Forecast observed = forecast.observe(new BigDecimal("67.00"));

        assertEquals(new BigDecimal("67.00"), observed.observedOccupancyPct());
        assertEquals(new BigDecimal("4.55"), observed.absoluteError());
    }
}

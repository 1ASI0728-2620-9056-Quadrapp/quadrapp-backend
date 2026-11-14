package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.AdvisoryPolicy;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastTrigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastDomainServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T15:02:10Z");
    private static final Instant SLOT = Instant.parse("2026-11-14T15:00:00Z");
    private final ForecastDomainService service = new ForecastDomainService(AdvisoryPolicy.DEFAULT);

    @ParameterizedTest
    @CsvSource({"0, 1.000", "2.5, 0.900", "5, 0.800", "12.5, 0.500", "25, 0.000", "30, 0.000"})
    void confidenceIsOneMinusMaeOver25ClampedToZeroAndOne(double mae, String expected) {
        assertThat(ForecastDomainService.confidenceFromMae(mae)).isEqualByComparingTo(expected);
    }

    @Test
    void aModelForecastKeepsTheModelVersionAndItsConfidenceLevel() {
        Forecast forecast = service.fromModel(7L, ForecastHorizon.THIRTY_MINUTES, NOW, UUID.randomUUID(), SLOT,
                ForecastTrigger.SCHEDULED, 104.2, 5.0, "model-v3", new BigDecimal("50"), 90);

        assertThat(forecast.method()).isEqualTo(ForecastMethod.PRIMARY_MODEL);
        assertThat(forecast.modelVersion()).isEqualTo("model-v3");
        assertThat(forecast.expectedOccupancyPct()).isEqualByComparingTo("100.00");
        assertThat(forecast.confidence()).isEqualByComparingTo("0.800");
        assertThat(forecast.confidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
        // From 50 % now to 100 % in 30 minutes, 90 % is reached after 24 minutes.
        assertThat(forecast.saturationExpectedAt()).isEqualTo(NOW.plusSeconds(24 * 60));
    }

    @Test
    void aFallbackForecastIsAlwaysLowConfidence() {
        Forecast forecast = service.fallback(7L, ForecastHorizon.FIFTEEN_MINUTES, NOW, UUID.randomUUID(), SLOT,
                ForecastTrigger.CAMPUS_EVENT, new BigDecimal("40"), "persistence-v1", new BigDecimal("40"), 90);

        assertThat(forecast.method()).isEqualTo(ForecastMethod.FALLBACK);
        assertThat(forecast.confidenceLevel()).isEqualTo(ConfidenceLevel.LOW);
        assertThat(forecast.confidence()).isEqualByComparingTo("0.000");
        assertThat(forecast.saturationExpectedAt()).isNull();
        assertThat(forecast.trigger()).isEqualTo(ForecastTrigger.CAMPUS_EVENT);
        assertThat(forecast.runSlot()).isEqualTo(SLOT);
    }
}

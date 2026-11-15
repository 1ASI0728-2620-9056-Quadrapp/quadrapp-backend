package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.AdvisoryPolicy;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalAdvice;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalCategory;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastTrigger;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArrivalAdvisorTest {

    private static final Instant GENERATED = Instant.parse("2026-11-14T15:00:00Z");
    private static final Instant NOW = GENERATED.plusSeconds(120);
    private final ArrivalAdvisor advisor = new ArrivalAdvisor(AdvisoryPolicy.DEFAULT);

    @Test
    void usesTheSmallestValidHorizonThatCoversTheEta() {
        List<Forecast> forecasts = List.of(
                forecast(15, "80", "0.900", ConfidenceLevel.HIGH, ForecastMethod.PRIMARY_MODEL),
                forecast(30, "95", "0.800", ConfidenceLevel.HIGH, ForecastMethod.PRIMARY_MODEL));

        ArrivalAdvice advice = advisor.advise(20, forecasts, 90, NOW).orElseThrow();

        assertThat(advice.horizonUsed()).isEqualTo(30);
        // 95 % expected with a 90 % threshold: (100 - 95) / (100 - 90) = 0.5.
        assertThat(advice.probabilityOfSpace()).isEqualByComparingTo("0.500");
        assertThat(advice.category()).isEqualTo(ArrivalCategory.LIMITED);
        assertThat(advice.confidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(advice.generatedAt()).isEqualTo(GENERATED);
    }

    @Test
    void anEtaBeyondTheLongestHorizonUsesItWithOneLevelLessConfidence() {
        List<Forecast> forecasts = List.of(forecast(60, "50", "0.900", ConfidenceLevel.HIGH,
                ForecastMethod.PRIMARY_MODEL));

        ArrivalAdvice advice = advisor.advise(120, forecasts, 90, NOW).orElseThrow();

        assertThat(advice.horizonUsed()).isEqualTo(60);
        assertThat(advice.probabilityOfSpace()).isEqualByComparingTo("1.000");
        assertThat(advice.category()).isEqualTo(ArrivalCategory.HIGH);
        assertThat(advice.confidenceLevel()).isEqualTo(ConfidenceLevel.MEDIUM);
    }

    @Test
    void aFallbackForecastGivesLowConfidenceAndAtMostLimited() {
        ArrivalAdvice advice = advisor.advise(0, List.of(forecast(15, "10", "0.000", ConfidenceLevel.LOW,
                ForecastMethod.FALLBACK)), 90, NOW).orElseThrow();

        assertThat(advice.category()).isEqualTo(ArrivalCategory.LIMITED);
        assertThat(advice.confidenceLevel()).isEqualTo(ConfidenceLevel.LOW);
    }

    @Test
    void expiredForecastsGiveNoAdviceAndTheEtaMustBeBetween0And120() {
        assertThat(advisor.advise(10, List.of(forecast(15, "10", "0.900", ConfidenceLevel.HIGH,
                ForecastMethod.PRIMARY_MODEL)), 90, GENERATED.plusSeconds(16 * 60))).isEmpty();
        assertThatThrownBy(() -> advisor.advise(121, List.of(), 90, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("etaMinutes must be between 0 and 120.");
        assertThatThrownBy(() -> advisor.advise(-1, List.of(), 90, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Forecast forecast(int horizon, String expected, String confidence, ConfidenceLevel level,
                                     ForecastMethod method) {
        return Forecast.create(1L, horizon, GENERATED, new BigDecimal(expected), new BigDecimal(confidence), level,
                method, "v", null, UUID.randomUUID(), GENERATED, ForecastTrigger.SCHEDULED);
    }
}

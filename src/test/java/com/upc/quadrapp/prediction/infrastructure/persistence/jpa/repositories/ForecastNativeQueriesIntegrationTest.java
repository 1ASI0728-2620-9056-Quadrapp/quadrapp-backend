package com.upc.quadrapp.prediction.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.infrastructure.persistence.jpa.entities.ForecastJpaEntity;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The forecast repository uses native SQL, which Hibernate validation never checks.
 * These queries must run against the prediction schema.
 */
@QuadrappIntegrationTest
@Transactional
class ForecastNativeQueriesIntegrationTest {

    private static final long PARKING_LOT_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-11-13T15:00:00Z");

    @Autowired
    private ForecastJpaRepository forecastRepository;

    @Test
    void findsTheLatestForecastOfEachHorizon() {
        forecastRepository.saveAndFlush(forecast((short) 15, NOW.minus(5, ChronoUnit.MINUTES), null));
        forecastRepository.saveAndFlush(forecast((short) 15, NOW, null));
        forecastRepository.saveAndFlush(forecast((short) 30, NOW, null));

        assertThat(forecastRepository.findLatestByParkingLot(PARKING_LOT_ID))
                .extracting(ForecastJpaEntity::getHorizonMinutes, ForecastJpaEntity::getGeneratedAt)
                .containsExactly(
                        tuple((short) 15, NOW),
                        tuple((short) 30, NOW));
    }

    @Test
    void averagesTheAbsoluteErrorOfEvaluatedForecasts() {
        forecastRepository.saveAndFlush(forecast((short) 15, NOW, new BigDecimal("4.00")));
        forecastRepository.saveAndFlush(forecast((short) 30, NOW, new BigDecimal("8.00")));
        forecastRepository.saveAndFlush(forecast((short) 45, NOW, null));

        ForecastJpaRepository.AccuracyMetricsProjection metrics =
                forecastRepository.accuracyMetricsByParkingLot(PARKING_LOT_ID);

        assertThat(metrics.getEvaluatedForecasts()).isEqualTo(2L);
        assertThat(metrics.getMeanAbsoluteErrorPct()).isEqualByComparingTo("6.00");
    }

    private ForecastJpaEntity forecast(short horizon, Instant generatedAt, BigDecimal absoluteError) {
        ForecastJpaEntity forecast = new ForecastJpaEntity();
        forecast.setParkingLotId(PARKING_LOT_ID);
        forecast.setHorizonMinutes(horizon);
        forecast.setGeneratedAt(generatedAt);
        forecast.setTargetAt(generatedAt.plus(horizon, ChronoUnit.MINUTES));
        forecast.setExpectedOccupancyPct(new BigDecimal("50.00"));
        forecast.setConfidence(new BigDecimal("0.800"));
        forecast.setConfidenceLevel(ConfidenceLevel.HIGH);
        forecast.setMethod(ForecastMethod.PRIMARY_MODEL);
        forecast.setModelVersion("linear-trend-v1");
        if (absoluteError != null) {
            forecast.setObservedOccupancyPct(new BigDecimal("50.00").add(absoluteError));
            forecast.setAbsoluteError(absoluteError);
        }
        return forecast;
    }
}

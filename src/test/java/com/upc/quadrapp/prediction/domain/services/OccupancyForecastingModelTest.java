package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastObservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OccupancyForecastingModelTest {

    private final OccupancyForecastingModel model = new OccupancyForecastingModel();
    private final Instant generatedAt = Instant.parse("2026-01-01T10:45:00Z");

    @Test
    void insufficientHistoryUsesLowConfidencePersistenceFallback() {
        var current = occupancy(10, 10, 80);

        var forecast = model.forecast(
                1L,
                ForecastHorizon.FIFTEEN_MINUTES,
                generatedAt,
                current,
                List.of(),
                configuration(70, List.of())
        );

        assertEquals(ForecastMethod.FALLBACK, forecast.method());
        assertEquals(ConfidenceLevel.LOW, forecast.confidenceLevel());
        assertEquals(BigDecimal.ZERO.setScale(3), forecast.confidence());
        assertEquals(new BigDecimal("50.00"), forecast.expectedOccupancyPct());
    }

    @Test
    void linearTrendUsesBacktestedErrorAndProjectsTheConfiguredHorizon() {
        var forecast = model.forecast(
                1L,
                ForecastHorizon.FIFTEEN_MINUTES,
                generatedAt,
                occupancy(40, 60, 0),
                List.of(
                        observation("2026-01-01T10:00:00Z", 10),
                        observation("2026-01-01T10:15:00Z", 20),
                        observation("2026-01-01T10:30:00Z", 30),
                        observation("2026-01-01T10:45:00Z", 40)
                ),
                configuration(70, List.of())
        );

        assertEquals(ForecastMethod.PRIMARY_MODEL, forecast.method());
        assertEquals(new BigDecimal("50.00"), forecast.expectedOccupancyPct());
        assertEquals(BigDecimal.ONE.setScale(3), forecast.confidence());
        assertEquals(ConfidenceLevel.HIGH, forecast.confidenceLevel());
        assertEquals(
                generatedAt.plusSeconds(45 * 60L),
                forecast.saturationExpectedAt()
        );
    }

    @Test
    void upcomingCalendarEventUsesFallbackInsteadOfInventingAnEventEffect() {
        var event = new ParkingConfigurationContextFacade.CalendarEventView(
                "EXAMS",
                "HIGH",
                generatedAt.plusSeconds(5 * 60L),
                generatedAt.plusSeconds(20 * 60L)
        );

        var forecast = model.forecast(
                1L,
                ForecastHorizon.FIFTEEN_MINUTES,
                generatedAt,
                occupancy(40, 60, 0),
                List.of(
                        observation("2026-01-01T10:00:00Z", 10),
                        observation("2026-01-01T10:15:00Z", 20),
                        observation("2026-01-01T10:30:00Z", 30),
                        observation("2026-01-01T10:45:00Z", 40)
                ),
                configuration(70, List.of(event))
        );

        assertEquals(ForecastMethod.FALLBACK, forecast.method());
        assertEquals(ConfidenceLevel.LOW, forecast.confidenceLevel());
        assertEquals("persistence-calendar-v1", forecast.modelVersion());
        assertEquals(new BigDecimal("40.00"), forecast.expectedOccupancyPct());
    }

    @Test
    void longerHorizonFallsBackUntilItHasMatchingBacktestSamples() {
        var forecast = model.forecast(
                1L,
                ForecastHorizon.THIRTY_MINUTES,
                generatedAt,
                occupancy(40, 60, 0),
                List.of(
                        observation("2026-01-01T10:00:00Z", 10),
                        observation("2026-01-01T10:15:00Z", 20),
                        observation("2026-01-01T10:30:00Z", 30),
                        observation("2026-01-01T10:45:00Z", 40)
                ),
                configuration(70, List.of())
        );

        assertEquals(ForecastMethod.FALLBACK, forecast.method());
        assertEquals(ConfidenceLevel.LOW, forecast.confidenceLevel());
    }

    private OccupancyContextFacade.OccupancyView occupancy(
            long occupied,
            long free,
            long unknown
    ) {
        return new OccupancyContextFacade.OccupancyView(
                "PARKING_LOT",
                1L,
                generatedAt,
                occupied + free + unknown,
                free,
                occupied,
                unknown
        );
    }

    private static ForecastObservation observation(String at, int occupancyPct) {
        return new ForecastObservation(
                Instant.parse(at),
                BigDecimal.valueOf(occupancyPct),
                BigDecimal.ONE
        );
    }

    private static ParkingConfigurationContextFacade.PredictionConfigurationView configuration(
            int saturationThresholdPct,
            List<ParkingConfigurationContextFacade.CalendarEventView> events
    ) {
        return new ParkingConfigurationContextFacade.PredictionConfigurationView(
                saturationThresholdPct,
                10,
                15,
                null,
                null,
                events
        );
    }
}

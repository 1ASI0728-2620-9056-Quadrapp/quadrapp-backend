package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastObservation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public final class OccupancyForecastingModel {

    private static final int MINIMUM_TRAINING_OBSERVATIONS = 4;
    private static final double MAXIMUM_FORECAST_MINUTES = 60.0;
    private static final BigDecimal ZERO_CONFIDENCE = BigDecimal.ZERO.setScale(3);

    public Forecast forecast(
            Long parkingLotId,
            ForecastHorizon horizon,
            Instant generatedAt,
            OccupancyContextFacade.OccupancyView current,
            List<ForecastObservation> observations,
            ParkingConfigurationContextFacade.PredictionConfigurationView configuration
    ) {
        BigDecimal currentOccupancy = occupancyPct(
                current.occupiedCount(),
                current.freeCount()
        );
        Instant targetAt = generatedAt.plusSeconds(horizon.minutes() * 60L);

        List<ForecastObservation> orderedObservations = observations.stream()
                .filter(observation -> !observation.takenAt().isAfter(generatedAt))
                .sorted(Comparator.comparing(ForecastObservation::takenAt))
                .toList();

        boolean calendarEventAffectsHorizon = configuration.calendarEvents()
                .stream()
                .anyMatch(event -> event.startsAt().isBefore(targetAt)
                        && event.endsAt().isAfter(generatedAt));

        if (calendarEventAffectsHorizon
                || orderedObservations.size() < MINIMUM_TRAINING_OBSERVATIONS) {
            return fallback(
                    parkingLotId,
                    horizon,
                    generatedAt,
                    currentOccupancy,
                    calendarEventAffectsHorizon
            );
        }

        List<Double> errors = rollingValidationErrors(
                orderedObservations,
                horizon.minutes()
        );
        if (errors.isEmpty()) {
            return fallback(
                    parkingLotId,
                    horizon,
                    generatedAt,
                    currentOccupancy,
                    false
            );
        }

        LinearTrend trend = fit(orderedObservations);
        double targetX = minutesBetween(
                orderedObservations.getFirst().takenAt(),
                targetAt
        );
        double predictedPct = clamp(trend.predict(targetX));

        double meanAbsoluteError = errors.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(100.0);
        double meanCoverage = orderedObservations.stream()
                .mapToDouble(observation -> observation.knownCoverage().doubleValue())
                .average()
                .orElse(0.0);
        double effectiveError = Math.min(
                100.0,
                meanAbsoluteError + (1.0 - meanCoverage) * 100.0
        );
        BigDecimal confidence = BigDecimal.valueOf(1.0 - effectiveError / 100.0)
                .max(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);

        int accuracyMarginPct = configuration.accuracyMarginPct();
        ConfidenceLevel confidenceLevel = effectiveError <= accuracyMarginPct
                ? ConfidenceLevel.HIGH
                : effectiveError <= accuracyMarginPct * 2.0
                        ? ConfidenceLevel.MEDIUM
                        : ConfidenceLevel.LOW;

        Instant saturationExpectedAt = saturationExpectedAt(
                generatedAt,
                orderedObservations.getFirst().takenAt(),
                trend,
                configuration.saturationThresholdPct()
        );

        return Forecast.create(
                parkingLotId,
                horizon.minutes(),
                generatedAt,
                BigDecimal.valueOf(predictedPct),
                confidence,
                confidenceLevel,
                ForecastMethod.PRIMARY_MODEL,
                "linear-trend-v1",
                saturationExpectedAt
        );
    }

    private static Forecast fallback(
            Long parkingLotId,
            ForecastHorizon horizon,
            Instant generatedAt,
            BigDecimal currentOccupancy,
            boolean calendarEventAffectsHorizon
    ) {
        return Forecast.create(
                parkingLotId,
                horizon.minutes(),
                generatedAt,
                currentOccupancy,
                ZERO_CONFIDENCE,
                ConfidenceLevel.LOW,
                ForecastMethod.FALLBACK,
                calendarEventAffectsHorizon
                        ? "persistence-calendar-v1"
                        : "persistence-v1",
                null
        );
    }

    private static List<Double> rollingValidationErrors(
            List<ForecastObservation> observations,
            int horizonMinutes
    ) {
        List<Double> errors = new ArrayList<>();
        Instant origin = observations.getFirst().takenAt();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumXY = 0.0;
        int nextValidationIndex = 0;

        for (int index = 0; index < observations.size(); index++) {
            ForecastObservation observation = observations.get(index);
            double x = minutesBetween(origin, observation.takenAt());
            double y = observation.occupancyPct().doubleValue();

            if (index >= 3) {
                Instant validationTarget = observations.get(index - 1).takenAt()
                        .plusSeconds(horizonMinutes * 60L);
                while (nextValidationIndex < observations.size()
                        && observations.get(nextValidationIndex).takenAt()
                        .isBefore(validationTarget)) {
                    nextValidationIndex++;
                }

                double variance = index * sumXX - sumX * sumX;
                if (nextValidationIndex < observations.size() && variance > 0.0) {
                    double slope = (index * sumXY - sumX * sumY) / variance;
                    double intercept = (sumY - slope * sumX) / index;
                    double targetX = minutesBetween(origin, validationTarget);
                    double prediction = intercept + slope * targetX;
                    double actual = observations.get(nextValidationIndex)
                            .occupancyPct()
                            .doubleValue();
                    errors.add(Math.abs(prediction - actual));
                }
            }

            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }
        return errors;
    }

    private static LinearTrend fit(List<ForecastObservation> observations) {
        Instant origin = observations.getFirst().takenAt();
        double meanX = observations.stream()
                .mapToDouble(value -> minutesBetween(origin, value.takenAt()))
                .average()
                .orElseThrow();
        double meanY = observations.stream()
                .mapToDouble(value -> value.occupancyPct().doubleValue())
                .average()
                .orElseThrow();

        double covariance = 0.0;
        double variance = 0.0;
        for (ForecastObservation observation : observations) {
            double x = minutesBetween(origin, observation.takenAt());
            double y = observation.occupancyPct().doubleValue();
            covariance += (x - meanX) * (y - meanY);
            variance += (x - meanX) * (x - meanX);
        }

        if (variance == 0.0) {
            throw new IllegalArgumentException(
                    "Forecast observations must have distinct timestamps."
            );
        }

        double slope = covariance / variance;
        return new LinearTrend(slope, meanY - slope * meanX);
    }

    private static Instant saturationExpectedAt(
            Instant generatedAt,
            Instant origin,
            LinearTrend trend,
            int saturationThresholdPct
    ) {
        double currentX = minutesBetween(origin, generatedAt);
        double currentPct = trend.predict(currentX);
        if (trend.slope() <= 0.0 || currentPct >= saturationThresholdPct) {
            return null;
        }

        double minutesToSaturation = (saturationThresholdPct - currentPct) / trend.slope();
        if (minutesToSaturation <= 0.0 || minutesToSaturation > MAXIMUM_FORECAST_MINUTES) {
            return null;
        }

        return generatedAt.plusMillis(Math.round(minutesToSaturation * 60_000.0));
    }

    private static BigDecimal occupancyPct(long occupied, long free) {
        long knownSpaces = occupied + free;
        if (knownSpaces == 0) {
            throw new IllegalStateException(
                    "Cannot forecast occupancy when all spaces are unknown."
            );
        }
        return BigDecimal.valueOf(occupied)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(knownSpaces), 2, RoundingMode.HALF_UP);
    }

    private static double minutesBetween(Instant start, Instant end) {
        return Duration.between(start, end).toMillis() / 60_000.0;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private record LinearTrend(double slope, double intercept) {
        private double predict(double x) {
            return intercept + slope * x;
        }
    }
}

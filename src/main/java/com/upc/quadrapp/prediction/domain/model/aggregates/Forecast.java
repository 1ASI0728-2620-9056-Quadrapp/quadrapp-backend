package com.upc.quadrapp.prediction.domain.model.aggregates;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public record Forecast(
        Long id,
        Long parkingLotId,
        int horizonMinutes,
        Instant generatedAt,
        Instant targetAt,
        BigDecimal expectedOccupancyPct,
        BigDecimal confidence,
        ConfidenceLevel confidenceLevel,
        ForecastMethod method,
        String modelVersion,
        Instant saturationExpectedAt,
        BigDecimal observedOccupancyPct,
        BigDecimal absoluteError
) {
    public Forecast {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Forecast ID must be positive.");
        }
        if (parkingLotId == null || parkingLotId <= 0) {
            throw new IllegalArgumentException("Parking lot ID must be positive.");
        }
        if (ForecastHorizon.all().stream().noneMatch(h -> h.minutes() == horizonMinutes)) {
            throw new IllegalArgumentException("Unsupported forecast horizon.");
        }
        Objects.requireNonNull(generatedAt);
        Objects.requireNonNull(targetAt);
        if (!targetAt.equals(generatedAt.plusSeconds(horizonMinutes * 60L))) {
            throw new IllegalArgumentException("Target time must match the forecast horizon.");
        }

        expectedOccupancyPct = normalizePercentage(expectedOccupancyPct, "Expected occupancy");
        confidence = Objects.requireNonNull(confidence).setScale(3, RoundingMode.HALF_UP);
        if (confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1.");
        }
        Objects.requireNonNull(confidenceLevel);
        Objects.requireNonNull(method);

        if (modelVersion == null || modelVersion.isBlank() || modelVersion.length() > 40) {
            throw new IllegalArgumentException("Model version must contain 1 to 40 characters.");
        }
        if (method == ForecastMethod.FALLBACK && confidenceLevel != ConfidenceLevel.LOW) {
            throw new IllegalArgumentException("Fallback forecasts must have LOW confidence.");
        }

        if ((observedOccupancyPct == null) != (absoluteError == null)) {
            throw new IllegalArgumentException(
                    "Observed occupancy and absolute error must be set together."
            );
        }
        if (observedOccupancyPct != null) {
            observedOccupancyPct = normalizePercentage(
                    observedOccupancyPct,
                    "Observed occupancy"
            );
            absoluteError = Objects.requireNonNull(absoluteError)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal calculatedError = expectedOccupancyPct
                    .subtract(observedOccupancyPct)
                    .abs()
                    .setScale(2, RoundingMode.HALF_UP);
            if (absoluteError.compareTo(calculatedError) != 0) {
                throw new IllegalArgumentException(
                        "Absolute error must equal the difference between expected and observed occupancy."
                );
            }
        }
    }

    public static Forecast create(
            Long parkingLotId,
            int horizonMinutes,
            Instant generatedAt,
            BigDecimal expectedOccupancyPct,
            BigDecimal confidence,
            ConfidenceLevel confidenceLevel,
            ForecastMethod method,
            String modelVersion,
            Instant saturationExpectedAt
    ) {
        return new Forecast(
                null,
                parkingLotId,
                horizonMinutes,
                generatedAt,
                generatedAt.plusSeconds(horizonMinutes * 60L),
                expectedOccupancyPct,
                confidence,
                confidenceLevel,
                method,
                modelVersion,
                saturationExpectedAt,
                null,
                null
        );
    }

    public Forecast observe(BigDecimal observedOccupancyPct) {
        BigDecimal observed = normalizePercentage(
                observedOccupancyPct,
                "Observed occupancy"
        );
        BigDecimal error = expectedOccupancyPct
                .subtract(observed)
                .abs()
                .setScale(2, RoundingMode.HALF_UP);
        return new Forecast(
                id,
                parkingLotId,
                horizonMinutes,
                generatedAt,
                targetAt,
                expectedOccupancyPct,
                confidence,
                confidenceLevel,
                method,
                modelVersion,
                saturationExpectedAt,
                observed,
                error
        );
    }

    private static BigDecimal normalizePercentage(BigDecimal value, String name) {
        BigDecimal normalized = Objects.requireNonNull(value, name + " is required.")
                .setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0
                || normalized.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 100.");
        }
        return normalized;
    }
}

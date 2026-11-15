package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.AdvisoryPolicy;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastTrigger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Builds forecasts from the model output or from the fallback heuristic (rule 6) and estimates
 * when the saturation threshold is reached.
 */
public class ForecastDomainService {

    /** MAE (percentage points) at which the model confidence reaches 0. */
    private static final double ZERO_CONFIDENCE_MAE = 25.0;
    private static final BigDecimal FALLBACK_CONFIDENCE = BigDecimal.ZERO.setScale(3);

    private final AdvisoryPolicy policy;

    public ForecastDomainService(AdvisoryPolicy policy) {
        this.policy = policy;
    }

    /** Confidence of a model forecast: {@code 1 - MAE / 25}, clamped to [0, 1]. */
    public static BigDecimal confidenceFromMae(double meanAbsoluteError) {
        double value = 1.0 - meanAbsoluteError / ZERO_CONFIDENCE_MAE;
        return BigDecimal.valueOf(Math.clamp(value, 0.0, 1.0)).setScale(3, RoundingMode.HALF_UP);
    }

    public Forecast fromModel(
            Long parkingLotId,
            ForecastHorizon horizon,
            Instant generatedAt,
            UUID runId,
            Instant runSlot,
            ForecastTrigger trigger,
            double expectedOccupancyPct,
            double meanAbsoluteError,
            String modelVersion,
            BigDecimal currentOccupancyPct,
            int saturationThresholdPct
    ) {
        BigDecimal expected = percentage(expectedOccupancyPct);
        BigDecimal confidence = confidenceFromMae(meanAbsoluteError);
        return Forecast.create(
                parkingLotId,
                horizon.minutes(),
                generatedAt,
                expected,
                confidence,
                policy.confidenceLevelFor(confidence.doubleValue()),
                ForecastMethod.PRIMARY_MODEL,
                modelVersion,
                saturationExpectedAt(currentOccupancyPct, expected, horizon, generatedAt, saturationThresholdPct),
                runId,
                runSlot,
                trigger
        );
    }

    public Forecast fallback(
            Long parkingLotId,
            ForecastHorizon horizon,
            Instant generatedAt,
            UUID runId,
            Instant runSlot,
            ForecastTrigger trigger,
            BigDecimal expectedOccupancyPct,
            String fallbackVersion,
            BigDecimal currentOccupancyPct,
            int saturationThresholdPct
    ) {
        BigDecimal expected = percentage(expectedOccupancyPct.doubleValue());
        return Forecast.create(
                parkingLotId,
                horizon.minutes(),
                generatedAt,
                expected,
                FALLBACK_CONFIDENCE,
                ConfidenceLevel.LOW,
                ForecastMethod.FALLBACK,
                fallbackVersion,
                saturationExpectedAt(currentOccupancyPct, expected, horizon, generatedAt, saturationThresholdPct),
                runId,
                runSlot,
                trigger
        );
    }

    /**
     * Moment the threshold is reached, interpolating linearly from the current occupancy (at
     * {@code generatedAt}) to the expected one (at the target time). Null when the forecast does
     * not reach the threshold; {@code generatedAt} when the lot is already saturated.
     */
    static Instant saturationExpectedAt(
            BigDecimal currentOccupancyPct,
            BigDecimal expectedOccupancyPct,
            ForecastHorizon horizon,
            Instant generatedAt,
            int saturationThresholdPct
    ) {
        double threshold = saturationThresholdPct;
        double expected = expectedOccupancyPct.doubleValue();
        if (expected < threshold) {
            return null;
        }
        long horizonSeconds = horizon.minutes() * 60L;
        if (currentOccupancyPct == null) {
            return generatedAt.plusSeconds(horizonSeconds);
        }
        double current = currentOccupancyPct.doubleValue();
        if (current >= threshold) {
            return generatedAt;
        }
        double fraction = (threshold - current) / (expected - current);
        return generatedAt.plusSeconds(Math.round(horizonSeconds * fraction));
    }

    private static BigDecimal percentage(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Expected occupancy must be a finite number.");
        }
        return BigDecimal.valueOf(Math.clamp(value, 0.0, 100.0)).setScale(2, RoundingMode.HALF_UP);
    }
}

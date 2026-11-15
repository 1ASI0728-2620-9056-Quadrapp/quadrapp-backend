package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.AdvisoryPolicy;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalAdvice;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalCategory;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Derives the arrival advice from the latest forecasts (rules 12 to 15 of section 5.5). */
public class ArrivalAdvisor {

    public static final int MAX_ETA_MINUTES = 120;

    private final AdvisoryPolicy policy;

    public ArrivalAdvisor(AdvisoryPolicy policy) {
        this.policy = policy;
    }

    /**
     * Uses the smallest valid horizon that covers the ETA; if none covers it, the longest valid
     * horizon with one confidence level less. Empty when no forecast is valid at {@code now}.
     */
    public Optional<ArrivalAdvice> advise(
            int etaMinutes,
            List<Forecast> forecasts,
            int saturationThresholdPct,
            Instant now
    ) {
        if (etaMinutes < 0 || etaMinutes > MAX_ETA_MINUTES) {
            throw new IllegalArgumentException("etaMinutes must be between 0 and 120.");
        }
        List<Forecast> valid = forecasts.stream()
                .filter(forecast -> forecast.targetAt().isAfter(now))
                .sorted(Comparator.comparingInt(Forecast::horizonMinutes))
                .toList();
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        Optional<Forecast> covering = valid.stream()
                .filter(forecast -> forecast.horizonMinutes() >= etaMinutes)
                .findFirst();
        Forecast used = covering.orElse(valid.getLast());

        double probability = probabilityOfSpace(used.expectedOccupancyPct().doubleValue(), saturationThresholdPct);
        ArrivalCategory category = policy.categoryFor(probability);
        ConfidenceLevel level = used.confidenceLevel();
        if (covering.isEmpty()) {
            level = lowerOneLevel(level);
        }
        if (used.method() == ForecastMethod.FALLBACK) {
            level = ConfidenceLevel.LOW;
            if (category == ArrivalCategory.HIGH) {
                category = ArrivalCategory.LIMITED;
            }
        }
        return Optional.of(new ArrivalAdvice(
                etaMinutes,
                BigDecimal.valueOf(probability).setScale(3, RoundingMode.HALF_UP),
                category,
                used.confidence(),
                level,
                used.horizonMinutes(),
                used.generatedAt()
        ));
    }

    /** 1 up to the saturation threshold, then down linearly to 0 at 100 %. */
    private static double probabilityOfSpace(double expectedPct, int thresholdPct) {
        if (expectedPct <= thresholdPct) {
            return 1.0;
        }
        if (thresholdPct >= 100) {
            return 0.0;
        }
        return Math.clamp((100.0 - expectedPct) / (100.0 - thresholdPct), 0.0, 1.0);
    }

    private static ConfidenceLevel lowerOneLevel(ConfidenceLevel level) {
        return level == ConfidenceLevel.HIGH ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW;
    }
}

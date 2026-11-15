package com.upc.quadrapp.prediction.interfaces.rest.resources;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalAdvice;

import java.math.BigDecimal;
import java.time.Instant;

public record ArrivalAdviceResource(
        int etaMinutes,
        BigDecimal probabilityOfSpace,
        String category,
        ConfidenceResource confidence,
        int horizonUsed,
        Instant generatedAt,
        Instant serverTime
) {
    public record ConfidenceResource(BigDecimal value, String level) {}

    public static ArrivalAdviceResource from(ArrivalAdvice advice, Instant serverTime) {
        return new ArrivalAdviceResource(
                advice.etaMinutes(),
                advice.probabilityOfSpace(),
                advice.category().name(),
                new ConfidenceResource(advice.confidence(), advice.confidenceLevel().name()),
                advice.horizonUsed(),
                advice.generatedAt(),
                serverTime
        );
    }
}

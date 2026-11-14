package com.upc.quadrapp.prediction.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Derived arrival advice for an ETA; computed on each request and never stored. */
public record ArrivalAdvice(
        int etaMinutes,
        BigDecimal probabilityOfSpace,
        ArrivalCategory category,
        BigDecimal confidence,
        ConfidenceLevel confidenceLevel,
        int horizonUsed,
        Instant generatedAt
) {
    public ArrivalAdvice {
        Objects.requireNonNull(probabilityOfSpace);
        Objects.requireNonNull(category);
        Objects.requireNonNull(confidence);
        Objects.requireNonNull(confidenceLevel);
        Objects.requireNonNull(generatedAt);
    }
}

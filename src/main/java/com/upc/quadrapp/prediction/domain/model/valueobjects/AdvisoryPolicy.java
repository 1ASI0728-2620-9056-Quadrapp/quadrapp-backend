package com.upc.quadrapp.prediction.domain.model.valueobjects;

/**
 * Thresholds that classify a probability of finding space and a confidence value. The initial
 * values are placeholders until the pilot data calibrates them (TS07).
 */
public record AdvisoryPolicy(
        double highFrom,
        double limitedFrom,
        double highConfidenceFrom,
        double mediumConfidenceFrom
) {
    public static final AdvisoryPolicy DEFAULT = new AdvisoryPolicy(0.7, 0.3, 0.8, 0.5);

    public AdvisoryPolicy {
        if (!(0 <= limitedFrom && limitedFrom < highFrom && highFrom <= 1)) {
            throw new IllegalArgumentException("Category thresholds must satisfy 0 <= limited < high <= 1.");
        }
        if (!(0 <= mediumConfidenceFrom && mediumConfidenceFrom < highConfidenceFrom && highConfidenceFrom <= 1)) {
            throw new IllegalArgumentException("Confidence thresholds must satisfy 0 <= medium < high <= 1.");
        }
    }

    public ArrivalCategory categoryFor(double probability) {
        if (probability >= highFrom) {
            return ArrivalCategory.HIGH;
        }
        return probability >= limitedFrom ? ArrivalCategory.LIMITED : ArrivalCategory.LOW;
    }

    public ConfidenceLevel confidenceLevelFor(double confidence) {
        if (confidence >= highConfidenceFrom) {
            return ConfidenceLevel.HIGH;
        }
        return confidence >= mediumConfidenceFrom ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW;
    }
}

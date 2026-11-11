package com.upc.quadrapp.parkingsensing.domain.model.valueobjects;

import java.util.Objects;

public record SensorReadingResult(
        ReadingOutcome outcome,
        SensorState confirmedState,
        PassDirection passageDirection
) {
    public SensorReadingResult {
        Objects.requireNonNull(outcome, "outcome is required.");
        if (outcome == ReadingOutcome.CONFIRMED && confirmedState == null) {
            throw new IllegalArgumentException("Confirmed reading requires state.");
        }
        if (outcome == ReadingOutcome.PASSAGE_RECORDED && passageDirection == null) {
            throw new IllegalArgumentException("Recorded passage requires direction.");
        }
    }
}

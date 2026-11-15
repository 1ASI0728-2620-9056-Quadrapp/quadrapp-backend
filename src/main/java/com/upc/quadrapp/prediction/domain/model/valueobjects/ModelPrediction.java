package com.upc.quadrapp.prediction.domain.model.valueobjects;

import java.util.Map;
import java.util.Objects;

/**
 * Raw output of the loaded model for one run: expected occupancy (%) and the model's mean
 * absolute error (percentage points) per horizon in minutes. A horizon the model did not return
 * is absent and is forecast with the fallback.
 */
public record ModelPrediction(
        String modelVersion,
        Map<Integer, Double> expectedOccupancyPctByHorizon,
        Map<Integer, Double> meanAbsoluteErrorByHorizon
) {
    public ModelPrediction {
        Objects.requireNonNull(modelVersion);
        expectedOccupancyPctByHorizon = Map.copyOf(expectedOccupancyPctByHorizon);
        meanAbsoluteErrorByHorizon = Map.copyOf(meanAbsoluteErrorByHorizon);
    }

    public boolean covers(int horizonMinutes) {
        Double expected = expectedOccupancyPctByHorizon.get(horizonMinutes);
        Double mae = meanAbsoluteErrorByHorizon.get(horizonMinutes);
        return expected != null && Double.isFinite(expected) && mae != null && Double.isFinite(mae) && mae >= 0;
    }
}

package com.upc.quadrapp.prediction.application.internal.outboundservices.model;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ModelPrediction;

import java.util.Map;
import java.util.Optional;

/**
 * The forecasting model, run in process. Empty when no model is loaded, the model needs a
 * feature that is missing, or the inference fails: the caller then uses the fallback (rule 6).
 */
public interface PredictionModelPort {

    /** Names of the features the backend can compute; a model may use any subset, in any order. */
    String OCCUPANCY_PCT = "occupancy_pct";
    String MINUTE_OF_DAY = "minute_of_day";
    String DAY_OF_WEEK = "day_of_week";
    String CAMPUS_EVENT = "campus_event";

    Optional<ModelPrediction> predict(Map<String, Double> features);
}

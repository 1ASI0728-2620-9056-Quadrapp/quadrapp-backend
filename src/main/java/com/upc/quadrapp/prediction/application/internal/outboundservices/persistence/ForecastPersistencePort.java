package com.upc.quadrapp.prediction.application.internal.outboundservices.persistence;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;

import java.time.Instant;
import java.util.List;

public interface ForecastPersistencePort {

    Forecast save(Forecast forecast);

    List<Forecast> findLatestByParkingLot(Long parkingLotId);

    /** The forecasts of the scheduled run of a parking lot in a 5-minute slot, by horizon. */
    List<Forecast> findScheduledRun(Long parkingLotId, Instant runSlot);

    /** Forecasts with {@code targetAt <= cutoff} not evaluated yet, oldest target first. */
    List<Forecast> findDueUnevaluated(Instant cutoff, int limit);

    /** Saves the observation of a forecast and marks it evaluated. */
    void saveObservation(Forecast observed, Instant evaluatedAt);

    /** Marks a forecast evaluated without an observation, so it leaves the queue. */
    void markUnobservable(Long forecastId, Instant evaluatedAt);

    int deleteGeneratedBefore(Instant cutoff);

    ForecastAccuracyMetrics accuracyMetricsByParkingLot(Long parkingLotId);
}

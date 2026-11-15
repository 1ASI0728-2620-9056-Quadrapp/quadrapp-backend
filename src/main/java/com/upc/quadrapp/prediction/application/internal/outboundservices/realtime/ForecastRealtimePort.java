package com.upc.quadrapp.prediction.application.internal.outboundservices.realtime;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;

import java.util.List;

/** Notifies the real-time subscribers of a parking lot that a run produced new forecasts. */
public interface ForecastRealtimePort {

    /** Sent after the surrounding transaction commits (at once when there is none). */
    void forecastsGenerated(Long parkingLotId, List<Forecast> forecasts);
}

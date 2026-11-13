package com.upc.quadrapp.prediction.application.internal.outboundservices.persistence;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;

import java.time.Instant;
import java.util.List;

public interface ForecastPersistencePort {

    Forecast save(Forecast forecast);

    List<Forecast> findLatestByParkingLot(Long parkingLotId);

    List<Forecast> findDueUnobserved(Instant now, int limit);

    ForecastAccuracyMetrics accuracyMetricsByParkingLot(Long parkingLotId);
}

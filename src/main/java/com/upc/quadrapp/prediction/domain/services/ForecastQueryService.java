package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;

import java.util.List;

public interface ForecastQueryService {

    List<Forecast> findLatestByParkingLot(Long parkingLotId);

    ForecastAccuracyMetrics accuracyMetricsByParkingLot(Long parkingLotId);
}

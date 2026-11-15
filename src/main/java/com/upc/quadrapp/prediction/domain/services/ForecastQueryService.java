package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalAdvice;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;

import java.util.List;

public interface ForecastQueryService {

    List<Forecast> findLatestByParkingLot(Long parkingLotId);

    ForecastAccuracyMetrics accuracyMetricsByParkingLot(Long parkingLotId);

    /**
     * Arrival advice for a parking lot the user can read, from its latest valid forecasts. Not
     * stored. 404 when the lot is missing or of another institution, or no forecast is valid.
     */
    ArrivalAdvice adviseArrival(Long parkingLotId, int etaMinutes);
}

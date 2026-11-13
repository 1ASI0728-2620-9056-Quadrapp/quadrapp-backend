package com.upc.quadrapp.prediction.application.internal.queryservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.application.internal.outboundservices.persistence.ForecastPersistencePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.exceptions.PredictionNotFoundException;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;
import com.upc.quadrapp.prediction.domain.services.ForecastQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastQueryServiceImpl implements ForecastQueryService {

    private final ForecastPersistencePort forecasts;
    private final ParkingConfigurationContextFacade parkingConfiguration;
    private final IamContextFacade iam;

    public ForecastQueryServiceImpl(
            ForecastPersistencePort forecasts,
            ParkingConfigurationContextFacade parkingConfiguration,
            IamContextFacade iam
    ) {
        this.forecasts = forecasts;
        this.parkingConfiguration = parkingConfiguration;
        this.iam = iam;
    }

    @Override
    public List<Forecast> findLatestByParkingLot(Long parkingLotId) {
        validateParkingLotAccess(parkingLotId);
        return forecasts.findLatestByParkingLot(parkingLotId);
    }

    @Override
    public ForecastAccuracyMetrics accuracyMetricsByParkingLot(Long parkingLotId) {
        validateParkingLotAccess(parkingLotId);
        return forecasts.accuracyMetricsByParkingLot(parkingLotId);
    }

    private void validateParkingLotAccess(Long parkingLotId) {
        Long institutionId = iam.getCurrentUser().institutionId();
        if (parkingConfiguration.findParkingLot(institutionId, parkingLotId).isEmpty()) {
            throw new PredictionNotFoundException("Parking lot not found.");
        }
    }
}

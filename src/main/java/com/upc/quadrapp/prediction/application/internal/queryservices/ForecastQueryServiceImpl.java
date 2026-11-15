package com.upc.quadrapp.prediction.application.internal.queryservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.application.internal.outboundservices.persistence.ForecastPersistencePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.exceptions.PredictionNotFoundException;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ArrivalAdvice;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;
import com.upc.quadrapp.prediction.domain.services.ArrivalAdvisor;
import com.upc.quadrapp.prediction.domain.services.ForecastQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ForecastQueryServiceImpl implements ForecastQueryService {

    private final ForecastPersistencePort forecasts;
    private final ParkingConfigurationContextFacade parkingConfiguration;
    private final IamContextFacade iam;
    private final ArrivalAdvisor advisor;
    private final Clock clock;

    public ForecastQueryServiceImpl(
            ForecastPersistencePort forecasts,
            ParkingConfigurationContextFacade parkingConfiguration,
            IamContextFacade iam,
            ArrivalAdvisor advisor,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.forecasts = forecasts;
        this.parkingConfiguration = parkingConfiguration;
        this.iam = iam;
        this.advisor = advisor;
        this.clock = clock;
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

    @Override
    public ArrivalAdvice adviseArrival(Long parkingLotId, int etaMinutes) {
        if (etaMinutes < 0 || etaMinutes > ArrivalAdvisor.MAX_ETA_MINUTES) {
            throw new IllegalArgumentException("etaMinutes must be between 0 and 120.");
        }
        // A lot of another institution reads as missing (same check as Parking Configuration).
        var lot = parkingConfiguration.findReadableParkingLot(parkingLotId)
                .orElseThrow(() -> new PredictionNotFoundException("Parking lot not found."));
        Instant now = clock.instant();
        var configuration = parkingConfiguration.findPredictionConfiguration(
                        lot.institutionId(), parkingLotId, now, now.plus(Duration.ofMinutes(60)))
                .orElseThrow(() -> new PredictionNotFoundException("Parking lot not found."));
        return advisor.advise(
                        etaMinutes,
                        forecasts.findLatestByParkingLot(parkingLotId),
                        configuration.saturationThresholdPct(),
                        now
                )
                .orElseThrow(() -> new PredictionNotFoundException("Arrival advice not available."));
    }

    private void validateParkingLotAccess(Long parkingLotId) {
        Long institutionId = iam.getCurrentUser().institutionId();
        if (parkingConfiguration.findParkingLot(institutionId, parkingLotId).isEmpty()) {
            throw new PredictionNotFoundException("Parking lot not found.");
        }
    }
}

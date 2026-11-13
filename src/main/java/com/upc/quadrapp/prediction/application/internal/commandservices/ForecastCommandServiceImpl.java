package com.upc.quadrapp.prediction.application.internal.commandservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.application.internal.outboundservices.persistence.ForecastPersistencePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.commands.GenerateForecastsCommand;
import com.upc.quadrapp.prediction.domain.model.exceptions.PredictionNotFoundException;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastObservation;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.prediction.domain.services.OccupancyForecastingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ForecastCommandServiceImpl implements ForecastCommandService {

    private static final int EVALUATION_BATCH_SIZE = 100;

    private final ForecastPersistencePort forecasts;
    private final OccupancyContextFacade occupancy;
    private final ParkingConfigurationContextFacade parkingConfiguration;
    private final IamContextFacade iam;
    private final OccupancyForecastingModel model;
    private final Clock clock;

    public ForecastCommandServiceImpl(
            ForecastPersistencePort forecasts,
            OccupancyContextFacade occupancy,
            ParkingConfigurationContextFacade parkingConfiguration,
            IamContextFacade iam,
            OccupancyForecastingModel model,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.forecasts = forecasts;
        this.occupancy = occupancy;
        this.parkingConfiguration = parkingConfiguration;
        this.iam = iam;
        this.model = model;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<Forecast> handle(GenerateForecastsCommand command) {
        Long institutionId = iam.getCurrentUser().institutionId();
        Instant configurationWindowStart = clock.instant();
        Instant configurationWindowEnd = configurationWindowStart.plusSeconds(60 * 60L);
        var configuration = parkingConfiguration.findPredictionConfiguration(
                        institutionId,
                        command.parkingLotId(),
                        configurationWindowStart,
                        configurationWindowEnd
                )
                .orElseThrow(
                        () -> new PredictionNotFoundException("Parking lot not found.")
                );

        List<OccupancyContextFacade.OccupancySnapshotView> capturedSnapshots =
                occupancy.captureSnapshotsByParkingLot(command.parkingLotId());
        Set<Long> currentZoneIds = capturedSnapshots.stream()
                .map(OccupancyContextFacade.OccupancySnapshotView::zoneId)
                .collect(Collectors.toUnmodifiableSet());

        OccupancyContextFacade.OccupancyView currentOccupancy =
                occupancy.occupancyByParkingLot(command.parkingLotId());
        List<OccupancyContextFacade.OccupancySnapshotView> snapshotHistory =
                occupancy.snapshotsByParkingLot(command.parkingLotId(), null, null);
        List<ForecastObservation> observations = toObservations(
                snapshotHistory,
                currentZoneIds
        );

        Instant generatedAt = clock.instant();
        return ForecastHorizon.all().stream()
                .map(horizon -> model.forecast(
                        command.parkingLotId(),
                        horizon,
                        generatedAt,
                        currentOccupancy,
                        observations,
                        configuration
                ))
                .map(forecasts::save)
                .toList();
    }

    @Override
    @Transactional
    public int evaluateDueForecasts() {
        Instant now = clock.instant();
        List<Forecast> due = forecasts.findDueUnobserved(now, EVALUATION_BATCH_SIZE);
        Map<Long, OccupancyContextFacade.OccupancyView> occupancyByLot = new HashMap<>();
        int evaluated = 0;

        for (Forecast forecast : due) {
            OccupancyContextFacade.OccupancyView current = occupancyByLot.computeIfAbsent(
                    forecast.parkingLotId(),
                    occupancy::currentOccupancyForInternalUse
            );
            long knownSpaces = current.freeCount() + current.occupiedCount();
            if (knownSpaces == 0) {
                continue;
            }

            BigDecimal observedOccupancy = BigDecimal.valueOf(current.occupiedCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(knownSpaces), 2, RoundingMode.HALF_UP);
            forecasts.save(forecast.observe(observedOccupancy));
            evaluated++;
        }

        return evaluated;
    }

    private static List<ForecastObservation> toObservations(
            List<OccupancyContextFacade.OccupancySnapshotView> snapshots,
            Set<Long> currentZoneIds
    ) {
        Map<Instant, List<OccupancyContextFacade.OccupancySnapshotView>> byTakenAt =
                snapshots.stream().collect(Collectors.groupingBy(
                        OccupancyContextFacade.OccupancySnapshotView::takenAt,
                        TreeMap::new,
                        Collectors.toList()
                ));

        return byTakenAt.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .map(OccupancyContextFacade.OccupancySnapshotView::zoneId)
                        .collect(Collectors.toSet())
                        .equals(currentZoneIds))
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .toList();
    }

    private static ForecastObservation aggregate(
            Instant takenAt,
            List<OccupancyContextFacade.OccupancySnapshotView> snapshots
    ) {
        long total = snapshots.stream()
                .mapToLong(OccupancyContextFacade.OccupancySnapshotView::totalSpaces)
                .sum();
        long free = snapshots.stream()
                .mapToLong(OccupancyContextFacade.OccupancySnapshotView::freeCount)
                .sum();
        long occupied = snapshots.stream()
                .mapToLong(OccupancyContextFacade.OccupancySnapshotView::occupiedCount)
                .sum();
        long known = free + occupied;
        if (total == 0 || known == 0) {
            return null;
        }

        BigDecimal occupancyPct = BigDecimal.valueOf(occupied)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(known), 2, RoundingMode.HALF_UP);
        BigDecimal knownCoverage = BigDecimal.valueOf(known)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        return new ForecastObservation(takenAt, occupancyPct, knownCoverage);
    }
}

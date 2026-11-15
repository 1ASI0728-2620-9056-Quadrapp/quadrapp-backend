package com.upc.quadrapp.prediction.application.internal.commandservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.application.internal.outboundservices.model.PredictionModelPort;
import com.upc.quadrapp.prediction.application.internal.outboundservices.persistence.ForecastPersistencePort;
import com.upc.quadrapp.prediction.application.internal.outboundservices.realtime.ForecastRealtimePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.commands.GenerateForecastsCommand;
import com.upc.quadrapp.prediction.domain.model.exceptions.PredictionNotFoundException;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastHorizon;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastTrigger;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ModelPrediction;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.prediction.domain.services.ForecastDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates forecast runs and evaluates due forecasts.
 * <p>
 * A run forecasts the four horizons of one parking lot with the loaded model; a horizon the model
 * does not cover, or every horizon when no model is loaded or the current occupancy is unknown,
 * uses the fallback (rule 6): the average of the same weekday and 15-minute slot over the last
 * 8 weeks, or the current occupancy with less than 2 weeks of history, always with LOW confidence.
 */
@Service
public class ForecastCommandServiceImpl implements ForecastCommandService {

    static final String WEEKLY_SLOT_AVERAGE_VERSION = "weekly-slot-average-v1";
    static final String CURRENT_OCCUPANCY_VERSION = "persistence-v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastCommandServiceImpl.class);
    private static final Duration RUN_SLOT = Duration.ofMinutes(5);
    private static final Duration FORECAST_WINDOW = Duration.ofMinutes(60);
    private static final Duration FALLBACK_SLOT = Duration.ofMinutes(15);
    private static final int FALLBACK_WEEKS = 8;
    private static final int MINIMUM_FALLBACK_WEEKS = 2;
    /** A forecast is evaluated once the snapshots around its target time exist. */
    private static final Duration EVALUATION_DELAY = Duration.ofMinutes(2);
    private static final Duration SNAPSHOT_TOLERANCE = Duration.ofMinutes(5);
    private static final int EVALUATION_BATCH_SIZE = 100;
    private static final Duration RETENTION = Duration.ofDays(7);

    private final ForecastPersistencePort forecasts;
    private final OccupancyContextFacade occupancy;
    private final ParkingConfigurationContextFacade parkingConfiguration;
    private final IamContextFacade iam;
    private final PredictionModelPort model;
    private final ForecastRealtimePort realtime;
    private final ForecastDomainService domain;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final ZoneId timeZone;

    public ForecastCommandServiceImpl(
            ForecastPersistencePort forecasts,
            OccupancyContextFacade occupancy,
            ParkingConfigurationContextFacade parkingConfiguration,
            IamContextFacade iam,
            PredictionModelPort model,
            ForecastRealtimePort realtime,
            ForecastDomainService domain,
            PlatformTransactionManager transactionManager,
            @Qualifier("parkingSensingClock") Clock clock,
            @Value("${quadrapp.prediction.time-zone:America/Lima}") ZoneId timeZone
    ) {
        this.forecasts = forecasts;
        this.occupancy = occupancy;
        this.parkingConfiguration = parkingConfiguration;
        this.iam = iam;
        this.model = model;
        this.realtime = realtime;
        this.domain = domain;
        // Every run and every evaluation commits on its own (REQUIRES_NEW), also when called from a
        // listener's transaction: a lot or forecast that fails never rolls back the others.
        TransactionTemplate ownTransaction = new TransactionTemplate(transactionManager);
        ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactions = ownTransaction;
        this.clock = clock;
        this.timeZone = timeZone;
    }

    @Override
    public List<Forecast> handle(GenerateForecastsCommand command) {
        Long institutionId = iam.getCurrentUser().institutionId();
        Instant now = clock.instant();
        var configuration = parkingConfiguration.findPredictionConfiguration(
                        institutionId,
                        command.parkingLotId(),
                        now,
                        now.plus(FORECAST_WINDOW)
                )
                .orElseThrow(() -> new PredictionNotFoundException("Parking lot not found."));
        LotProfile lot = new LotProfile(
                command.parkingLotId(),
                institutionId,
                configuration.saturationThresholdPct(),
                !configuration.calendarEvents().isEmpty()
        );
        return generateRun(lot, ForecastTrigger.SCHEDULED, now)
                .orElseGet(() -> forecasts.findScheduledRun(lot.parkingLotId(), runSlot(now)));
    }

    @Override
    public int generateScheduledRuns() {
        Instant now = clock.instant();
        int runs = 0;
        for (Long parkingLotId : parkingConfiguration.activeParkingLotIds()) {
            try {
                Optional<LotProfile> lot = profileOf(parkingLotId, now);
                if (lot.isPresent() && generateRun(lot.get(), ForecastTrigger.SCHEDULED, now).isPresent()) {
                    runs++;
                }
            } catch (IllegalStateException exception) {
                LOGGER.debug("No forecast for parking lot {}: {}", parkingLotId, exception.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.warn("Forecast run failed for parking lot {}.", parkingLotId, exception);
            }
        }
        return runs;
    }

    @Override
    public int regenerateForCampusEvent(Long institutionId, Long parkingLotId, Instant startAt, Instant endAt) {
        Instant now = clock.instant();
        if (!startAt.isBefore(now.plus(FORECAST_WINDOW)) || !endAt.isAfter(now)) {
            return 0;
        }
        List<Long> candidates = parkingLotId != null
                ? List.of(parkingLotId)
                : parkingConfiguration.activeParkingLotIds();
        int runs = 0;
        for (Long candidate : candidates) {
            // One lot that cannot be forecast never stops the others; the listener always completes.
            try {
                Optional<LotProfile> lot = profileOf(candidate, now)
                        .filter(profile -> profile.institutionId().equals(institutionId));
                if (lot.isEmpty()) {
                    continue;
                }
                generateRun(lot.get(), ForecastTrigger.CAMPUS_EVENT, now);
                runs++;
            } catch (IllegalStateException exception) {
                LOGGER.debug("No forecast for parking lot {}: {}", candidate, exception.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.warn("Campus-event forecast run failed for parking lot {}.", candidate, exception);
            }
        }
        return runs;
    }

    @Override
    public int evaluateDueForecasts() {
        Instant now = clock.instant();
        List<Forecast> due = forecasts.findDueUnevaluated(now.minus(EVALUATION_DELAY), EVALUATION_BATCH_SIZE);
        int evaluated = 0;
        // Each forecast in its own transaction: one that fails never rolls back the others.
        for (Forecast forecast : due) {
            try {
                if (Boolean.TRUE.equals(transactions.execute(status -> evaluate(forecast, now)))) {
                    evaluated++;
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Forecast {} could not be evaluated; it is marked unobservable.", forecast.id(), exception);
                try {
                    transactions.executeWithoutResult(status -> forecasts.markUnobservable(forecast.id(), now));
                } catch (RuntimeException markFailure) {
                    LOGGER.error("Forecast {} could not be marked unobservable.", forecast.id(), markFailure);
                }
            }
        }
        return evaluated;
    }

    @Override
    public int purgeExpiredForecasts() {
        Instant cutoff = clock.instant().minus(RETENTION);
        Integer deleted = transactions.execute(status -> forecasts.deleteGeneratedBefore(cutoff));
        return deleted == null ? 0 : deleted;
    }

    /**
     * Compares the forecast with the lot snapshot closest to its target time. Without one, or
     * with no known space, the forecast is marked evaluated without an observation so it leaves
     * the queue.
     */
    private boolean evaluate(Forecast forecast, Instant now) {
        Optional<BigDecimal> observed = occupancy
                .lotOccupancyAt(forecast.parkingLotId(), forecast.targetAt(), SNAPSHOT_TOLERANCE)
                .map(ForecastCommandServiceImpl::occupancyPct);
        if (observed.isPresent() && observed.get() != null) {
            forecasts.saveObservation(forecast.observe(observed.get()), now);
            return true;
        }
        forecasts.markUnobservable(forecast.id(), now);
        return false;
    }

    /**
     * Generates and saves one run. Empty when the scheduled run of this slot already exists (for
     * example, saved by another replica).
     *
     * @throws IllegalStateException when no horizon has a usable reading (rule 7)
     */
    private Optional<List<Forecast>> generateRun(LotProfile lot, ForecastTrigger trigger, Instant now) {
        Instant slot = runSlot(now);
        try {
            return transactions.execute(status -> {
                if (trigger == ForecastTrigger.SCHEDULED
                        && !forecasts.findScheduledRun(lot.parkingLotId(), slot).isEmpty()) {
                    return Optional.<List<Forecast>>empty();
                }
                List<Forecast> saved = forecastsOf(lot, trigger, now, slot).stream()
                        .map(forecasts::save)
                        .toList();
                if (saved.isEmpty()) {
                    throw new IllegalStateException("Cannot forecast occupancy when all spaces are unknown.");
                }
                realtime.forecastsGenerated(lot.parkingLotId(), saved);
                return Optional.of(saved);
            });
        } catch (DataIntegrityViolationException exception) {
            if (trigger == ForecastTrigger.SCHEDULED) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private List<Forecast> forecastsOf(LotProfile lot, ForecastTrigger trigger, Instant now, Instant slot) {
        BigDecimal currentPct = occupancyPct(occupancy.currentOccupancyForInternalUse(lot.parkingLotId()));
        Optional<ModelPrediction> prediction = currentPct == null
                ? Optional.empty()
                : predict(features(currentPct, now, lot.campusEventAhead()));
        UUID runId = UUID.randomUUID();

        List<Forecast> result = new ArrayList<>();
        for (ForecastHorizon horizon : ForecastHorizon.all()) {
            if (prediction.isPresent() && prediction.get().covers(horizon.minutes())) {
                ModelPrediction output = prediction.get();
                result.add(domain.fromModel(
                        lot.parkingLotId(), horizon, now, runId, slot, trigger,
                        output.expectedOccupancyPctByHorizon().get(horizon.minutes()),
                        output.meanAbsoluteErrorByHorizon().get(horizon.minutes()),
                        output.modelVersion(), currentPct, lot.saturationThresholdPct()
                ));
                continue;
            }
            fallbackOf(lot, horizon, now, currentPct).ifPresent(fallback -> result.add(domain.fallback(
                    lot.parkingLotId(), horizon, now, runId, slot, trigger,
                    fallback.expectedPct(), fallback.version(), currentPct, lot.saturationThresholdPct()
            )));
        }
        return result;
    }

    /** A model that fails gives no prediction, so the run uses the fallback (rule 6). */
    private Optional<ModelPrediction> predict(Map<String, Double> features) {
        try {
            return model.predict(features);
        } catch (RuntimeException exception) {
            LOGGER.warn("The forecasting model failed; the fallback is used.", exception);
            return Optional.empty();
        }
    }

    private Optional<Fallback> fallbackOf(LotProfile lot, ForecastHorizon horizon, Instant now, BigDecimal currentPct) {
        Instant target = now.plus(Duration.ofMinutes(horizon.minutes()));
        var history = occupancy.weeklySlotAverage(
                lot.parkingLotId(), floor(target, FALLBACK_SLOT), FALLBACK_SLOT, FALLBACK_WEEKS);
        if (history.weeksWithData() >= MINIMUM_FALLBACK_WEEKS && history.averageOccupancyPct() != null) {
            return Optional.of(new Fallback(history.averageOccupancyPct(), WEEKLY_SLOT_AVERAGE_VERSION));
        }
        return Optional.ofNullable(currentPct).map(pct -> new Fallback(pct, CURRENT_OCCUPANCY_VERSION));
    }

    private Map<String, Double> features(BigDecimal currentPct, Instant now, boolean campusEventAhead) {
        ZonedDateTime local = now.atZone(timeZone);
        return Map.of(
                PredictionModelPort.OCCUPANCY_PCT, currentPct.doubleValue(),
                PredictionModelPort.MINUTE_OF_DAY, (double) (local.getHour() * 60 + local.getMinute()),
                PredictionModelPort.DAY_OF_WEEK, (double) local.getDayOfWeek().getValue(),
                PredictionModelPort.CAMPUS_EVENT, campusEventAhead ? 1.0 : 0.0
        );
    }

    private Optional<LotProfile> profileOf(Long parkingLotId, Instant now) {
        return parkingConfiguration.predictionProfileOf(parkingLotId, now, now.plus(FORECAST_WINDOW))
                .filter(ParkingConfigurationContextFacade.PredictionProfileView::active)
                .map(profile -> new LotProfile(
                        profile.parkingLotId(),
                        profile.institutionId(),
                        profile.saturationThresholdPct(),
                        !profile.campusEvents().isEmpty()
                ));
    }

    /** Occupied / known spaces in %, or null when no space is known. */
    private static BigDecimal occupancyPct(OccupancyContextFacade.OccupancyView view) {
        long known = view.freeCount() + view.occupiedCount();
        if (known == 0) {
            return null;
        }
        return BigDecimal.valueOf(view.occupiedCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(known), 2, RoundingMode.HALF_UP);
    }

    static Instant runSlot(Instant now) {
        return floor(now, RUN_SLOT);
    }

    private static Instant floor(Instant instant, Duration step) {
        long seconds = step.toSeconds();
        return Instant.ofEpochSecond(Math.floorDiv(instant.getEpochSecond(), seconds) * seconds);
    }

    private record LotProfile(
            Long parkingLotId,
            Long institutionId,
            int saturationThresholdPct,
            boolean campusEventAhead
    ) {}

    private record Fallback(BigDecimal expectedPct, String version) {}
}

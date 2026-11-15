package com.upc.quadrapp.prediction.application.internal.commandservices;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventProvided;
import com.upc.quadrapp.prediction.application.internal.outboundservices.model.PredictionModelPort;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ModelPrediction;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.upc.quadrapp.occupancy.OccupancyTestLayout.space;
import static com.upc.quadrapp.prediction.PredictionTestSupport.occupy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Each parking lot is forecast in its own transaction: a lot that cannot be forecast (no known
 * space and no history) or whose run fails never rolls back the runs of the other lots, also when
 * the runs come from a campus event listener. A model that throws or returns a non-finite value
 * falls back for that run or horizon (rule 6) instead of failing.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class ForecastRunIsolationIntegrationTest {

    @Autowired
    private ForecastCommandService forecasts;

    @Autowired
    private OccupancyCommandService occupancy;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transactions;

    @MockitoBean
    private PredictionModelPort model;

    private OccupancyTestLayout lot;
    private long unreadLot;

    @BeforeEach
    void twoLotsOfOneInstitution() {
        when(model.predict(any())).thenReturn(Optional.empty());
        lot = OccupancyTestLayout.create(jdbc, 4, 10);
        occupy(occupancy, lot, 0);
        // A second lot of the same institution whose only space has never been read.
        unreadLot = jdbc.queryForObject("insert into configuration.parking_lots (accuracy_margin_pct, active, "
                + "flow_window_minutes, freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name, "
                + "low_availability_pct) values (5, true, 15, 10, 1, 20, 10, 90, 300, now(), ?, now(), ?, 10) "
                + "returning id", Long.class, lot.institutionId, "Unread " + lot.parkingLotId);
        long zone = jdbc.queryForObject("insert into configuration.zones (lot_id, name, sort_order, created_at, "
                + "updated_at) values (?, 'South', 0, now(), now()) returning id", Long.class, unreadLot);
        long level = jdbc.queryForObject("insert into configuration.levels (zone_id, name, sort_order, created_at, "
                + "updated_at) values (?, 'L1', 0, now(), now()) returning id", Long.class, zone);
        space(jdbc, level, "U-1", true);
    }

    @Test
    void aCampusEventRegeneratesTheLotsThatCanBeForecastEvenIfAnotherOneCannot() {
        Instant now = Instant.now();
        transactions.executeWithoutResult(status -> events.publishEvent(new CampusEventProvided(9L,
                lot.institutionId, null, now.plus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS), "HIGH")));

        long deadline = System.currentTimeMillis() + 10_000;
        while (rows(lot.parkingLotId).size() < 4) {
            assertThat(System.currentTimeMillis()).as("forecasts of the readable lot").isLessThan(deadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
        assertThat(rows(lot.parkingLotId)).extracting(row -> row.get("trigger")).containsOnly("CAMPUS_EVENT");
        assertThat(rows(unreadLot)).isEmpty();
        while (incompletePublications() > 0) {
            assertThat(System.currentTimeMillis()).as("listener publication completed").isLessThan(deadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
    }

    @Test
    void aModelThatThrowsFallsBackForTheWholeRun() {
        when(model.predict(any())).thenThrow(new IllegalStateException("Inference failed."));

        forecasts.generateScheduledRuns();

        assertThat(rows(lot.parkingLotId)).hasSize(4)
                .extracting(row -> row.get("method")).containsOnly("FALLBACK");
    }

    @Test
    void aNonFiniteModelValueFallsBackForThatHorizonOnly() {
        when(model.predict(any())).thenReturn(Optional.of(new ModelPrediction("m-v1",
                Map.of(15, Double.NaN, 30, 40.0, 45, Double.POSITIVE_INFINITY, 60, 60.0),
                Map.of(15, 2.0, 30, 2.0, 45, 2.0, 60, 2.0))));

        forecasts.generateScheduledRuns();

        assertThat(rows(lot.parkingLotId)).extracting(row -> row.get("method"))
                .containsExactly("FALLBACK", "PRIMARY_MODEL", "FALLBACK", "PRIMARY_MODEL");
    }

    private long incompletePublications() {
        return jdbc.queryForObject("select count(*) from shared.event_publication where completion_date is null "
                + "and event_type like '%CampusEventProvided'", Long.class);
    }

    private List<Map<String, Object>> rows(long parkingLotId) {
        return jdbc.queryForList("select * from prediction.forecasts where lot_id = ? order by horizon_min",
                parkingLotId);
    }
}

package com.upc.quadrapp.prediction.application.internal.commandservices;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static com.upc.quadrapp.prediction.PredictionTestSupport.occupy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * A due forecast is compared with the minute snapshot closest to its target time, not with the
 * occupancy at evaluation time. A forecast that cannot be observed (no snapshot, no known space)
 * or whose evaluation fails is marked evaluated without an observation, so it never blocks the
 * queue, and it does not roll back the other forecasts of the batch.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class ForecastEvaluationIntegrationTest {

    @Autowired
    private ForecastCommandService forecasts;

    @Autowired
    private OccupancyCommandService occupancy;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private OccupancyContextFacade occupancyFacade;

    private OccupancyTestLayout lot;
    private OccupancyTestLayout failing;

    @BeforeEach
    void lots() {
        jdbc.update("update prediction.forecasts set evaluated_at = now() where evaluated_at is null");
        lot = OccupancyTestLayout.create(jdbc, 4, 10);
        failing = OccupancyTestLayout.create(jdbc, 4, 10);
        occupy(occupancy, lot, 0);
        doThrow(new IllegalStateException("Occupancy is unavailable."))
                .when(occupancyFacade).lotOccupancyAt(eq(failing.parkingLotId), any(), any());
    }

    @Test
    void dueForecastsAreObservedAtTheirTargetTimeAndUnobservableOnesDoNotBlockTheQueue() {
        Instant now = Instant.now();
        Instant observedTarget = now.minus(10, ChronoUnit.MINUTES);
        long noSnapshot = forecast(lot.parkingLotId, now.minus(40, ChronoUnit.MINUTES));
        long failingOne = forecast(failing.parkingLotId, now.minus(35, ChronoUnit.MINUTES));
        long observed = forecast(lot.parkingLotId, observedTarget);
        long notDue = forecast(lot.parkingLotId, now.minusSeconds(30));
        // Three of four spaces were occupied at the target time; one of four is occupied now.
        snapshot(lot, observedTarget.plusSeconds(20), 1, 3);
        snapshot(lot, observedTarget.minus(4, ChronoUnit.MINUTES), 4, 0);

        int evaluated = forecasts.evaluateDueForecasts();

        assertThat(evaluated).isEqualTo(1);
        Map<String, Object> row = row(observed);
        assertThat((BigDecimal) row.get("observed_occupancy_pct")).isEqualByComparingTo("75.00");
        assertThat((BigDecimal) row.get("abs_error")).isEqualByComparingTo("25.00");
        assertThat(row.get("evaluated_at")).isNotNull();
        for (long unobservable : new long[]{noSnapshot, failingOne}) {
            assertThat(row(unobservable).get("observed_occupancy_pct")).isNull();
            assertThat(row(unobservable).get("evaluated_at")).isNotNull();
        }
        assertThat(row(notDue).get("evaluated_at")).isNull();

        assertThat(forecasts.evaluateDueForecasts()).isZero();
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap("select * from prediction.forecasts where id = ?", id);
    }

    private long forecast(long parkingLotId, Instant targetAt) {
        Instant generatedAt = targetAt.minus(15, ChronoUnit.MINUTES);
        return jdbc.queryForObject("insert into prediction.forecasts (lot_id, horizon_min, generated_at, target_at, "
                        + "expected_occupancy_pct, confidence, confidence_level, method, model_version, run_id, "
                        + "run_slot, trigger) values (?, 15, ?, ?, 50, 0, 'LOW', 'FALLBACK', 'persistence-v1', ?, ?, "
                        + "'CAMPUS_EVENT') returning id", Long.class,
                parkingLotId, generatedAt.atOffset(ZoneOffset.UTC), targetAt.atOffset(ZoneOffset.UTC),
                UUID.randomUUID(), generatedAt.atOffset(ZoneOffset.UTC));
    }

    private void snapshot(OccupancyTestLayout layout, Instant takenAt, int free, int occupied) {
        jdbc.update("insert into occupancy.occupancy_snapshots (parking_lot_id, zone_id, taken_at, total_spaces, "
                        + "free_count, occupied_count, unknown_count) values (?, ?, ?, ?, ?, ?, 0)",
                layout.parkingLotId, layout.zoneId, takenAt.atOffset(ZoneOffset.UTC), free + occupied, free, occupied);
    }
}

package com.upc.quadrapp.prediction.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventCancelled;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventProvided;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.upc.quadrapp.prediction.PredictionTestSupport.history;
import static com.upc.quadrapp.prediction.PredictionTestSupport.occupy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Forecast generation runs without a model loaded (the fallback, rule 6): runs per 5-minute slot,
 * their constraints, the weekly slot average, campus-event regeneration and the 7-day purge.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class ForecastRunsIntegrationTest {

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

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CurrentUserService currentUser;

    private OccupancyTestLayout lot;

    @BeforeEach
    void lotWithFourSpaces() {
        lot = OccupancyTestLayout.create(jdbc, 4, 10);
        when(currentUser.requireAuthenticatedUserId()).thenReturn(lot.userId);
    }

    @Test
    void aScheduledRunSavesOneForecastPerHorizonOncePerSlot() {
        occupy(occupancy, lot, 0);

        forecasts.generateScheduledRuns();
        forecasts.generateScheduledRuns();

        List<Map<String, Object>> rows = rows(lot.parkingLotId);
        assertThat(rows).extracting(row -> ((Number) row.get("horizon_min")).intValue())
                .containsExactly(15, 30, 45, 60);
        assertThat(rows).extracting(row -> row.get("run_id")).containsOnly(rows.getFirst().get("run_id"));
        assertThat(rows).extracting(row -> row.get("trigger")).containsOnly("SCHEDULED");
        Instant generatedAt = ((Timestamp) rows.getFirst().get("generated_at")).toInstant();
        Instant runSlot = ((Timestamp) rows.getFirst().get("run_slot")).toInstant();
        assertThat(runSlot).isEqualTo(Instant.ofEpochSecond(generatedAt.getEpochSecond() / 300 * 300));
        // Less than 2 weeks of history: the current occupancy (1 of 4) with LOW confidence.
        assertThat(rows).allSatisfy(row -> {
            assertThat((BigDecimal) row.get("expected_occupancy_pct")).isEqualByComparingTo("25.00");
            assertThat(row.get("method")).isEqualTo("FALLBACK");
            assertThat(row.get("confidence_level")).isEqualTo("LOW");
            assertThat(row.get("model_version")).isEqualTo("persistence-v1");
        });
    }

    @Test
    void anInactiveLotIsNotForecast() {
        occupy(occupancy, lot, 0);
        jdbc.update("update configuration.parking_lots set active = false where id = ?", lot.parkingLotId);

        forecasts.generateScheduledRuns();

        assertThat(rows(lot.parkingLotId)).isEmpty();
    }

    @Test
    void twoWeeksOfHistoryUseTheAverageOfTheSameWeekdayAndSlot() {
        occupy(occupancy, lot, 0);
        history(jdbc, lot, 1, 1, 3, 80);
        history(jdbc, lot, 2, 1, 3, 80);

        forecasts.generateScheduledRuns();

        assertThat(rows(lot.parkingLotId)).hasSize(4).allSatisfy(row -> {
            assertThat((BigDecimal) row.get("expected_occupancy_pct")).isEqualByComparingTo("75.00");
            assertThat(row.get("method")).isEqualTo("FALLBACK");
            assertThat(row.get("confidence_level")).isEqualTo("LOW");
            assertThat(row.get("model_version")).isEqualTo("weekly-slot-average-v1");
        });
    }

    @Test
    void oneWeekOfHistoryIsNotEnoughForTheAverage() {
        occupy(occupancy, lot, 0, 1);
        history(jdbc, lot, 1, 1, 3, 80);

        forecasts.generateScheduledRuns();

        assertThat(rows(lot.parkingLotId)).hasSize(4).allSatisfy(row ->
                assertThat((BigDecimal) row.get("expected_occupancy_pct")).isEqualByComparingTo("50.00"));
    }

    @Test
    void allSpacesUnknownUseTheHistoryWithLowConfidenceOrAnswer409WithoutIt() throws Exception {
        mvc.perform(post("/api/v1/parking-lots/{id}/forecasts", lot.parkingLotId).with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot forecast occupancy when all spaces are unknown."));

        history(jdbc, lot, 1, 2, 2, 80);
        history(jdbc, lot, 2, 2, 2, 80);

        mvc.perform(post("/api/v1/parking-lots/{id}/forecasts", lot.parkingLotId).with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].expectedOccupancyPct").value(50.0))
                .andExpect(jsonPath("$[0].confidenceLevel").value("LOW"))
                .andExpect(jsonPath("$[0].method").value("FALLBACK"))
                .andExpect(jsonPath("$[0].trigger").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].runId").isNotEmpty())
                .andExpect(jsonPath("$[0].runSlot").isNotEmpty());
        mvc.perform(get("/api/v1/parking-lots/{id}/forecasts", lot.parkingLotId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].modelVersion").value("weekly-slot-average-v1"));
    }

    @Test
    void theDatabaseRejectsASecondScheduledRunInTheSameSlotButNotACampusEventRun() {
        Instant slot = Instant.parse("2026-11-14T10:05:00Z");
        insert(lot.parkingLotId, UUID.randomUUID(), slot, "SCHEDULED", slot);
        insert(lot.parkingLotId, UUID.randomUUID(), slot, "CAMPUS_EVENT", slot.plusSeconds(1));

        assertThatThrownBy(() -> insert(lot.parkingLotId, UUID.randomUUID(), slot, "SCHEDULED", slot.plusSeconds(2)))
                .isInstanceOf(DataIntegrityViolationException.class);
        UUID run = UUID.randomUUID();
        insert(lot.parkingLotId, run, slot.plusSeconds(300), "CAMPUS_EVENT", slot.plusSeconds(301));
        assertThatThrownBy(() -> insert(lot.parkingLotId, run, slot.plusSeconds(300), "CAMPUS_EVENT",
                slot.plusSeconds(302))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aCampusEventInTheNextHourRegeneratesTheForecastsAndAFarOneDoesNot() {
        occupy(occupancy, lot, 0);
        Instant now = Instant.now();

        publish(new CampusEventProvided(1L, lot.institutionId, lot.parkingLotId,
                now.plus(3, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS), "HIGH"));
        publish(new CampusEventCancelled(2L, lot.institutionId, null,
                now.plus(20, ChronoUnit.MINUTES), now.plus(2, ChronoUnit.HOURS)));

        awaitRows(lot.parkingLotId, 4);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
        assertThat(rows(lot.parkingLotId)).hasSize(4)
                .extracting(row -> row.get("trigger")).containsOnly("CAMPUS_EVENT");

        publish(new CampusEventProvided(3L, lot.institutionId, lot.parkingLotId,
                now.plus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS), "HIGH"));
        awaitRows(lot.parkingLotId, 8);
    }

    @Test
    void forecastsOlderThanSevenDaysArePurged() {
        Instant old = Instant.now().minus(8, ChronoUnit.DAYS);
        Instant recent = Instant.now().minus(6, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        insert(lot.parkingLotId, UUID.randomUUID(), old, "SCHEDULED", old);
        insert(lot.parkingLotId, UUID.randomUUID(), recent, "SCHEDULED", recent);

        forecasts.purgeExpiredForecasts();

        assertThat(rows(lot.parkingLotId)).extracting(row -> ((Timestamp) row.get("generated_at")).toInstant())
                .containsExactly(recent);
    }

    private void publish(Object event) {
        transactions.executeWithoutResult(status -> events.publishEvent(event));
    }

    private void awaitRows(long parkingLotId, int count) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (rows(parkingLotId).size() < count) {
            assertThat(System.currentTimeMillis()).as("forecasts regenerated").isLessThan(deadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
    }

    private List<Map<String, Object>> rows(long parkingLotId) {
        return jdbc.queryForList("select * from prediction.forecasts where lot_id = ? order by generated_at, horizon_min",
                parkingLotId);
    }

    private void insert(long parkingLotId, UUID runId, Instant runSlot, String trigger, Instant generatedAt) {
        jdbc.update("insert into prediction.forecasts (lot_id, horizon_min, generated_at, target_at, "
                        + "expected_occupancy_pct, confidence, confidence_level, method, model_version, run_id, "
                        + "run_slot, trigger) values (?, 15, ?, ?, 50, 0, 'LOW', 'FALLBACK', 'persistence-v1', ?, ?, ?)",
                parkingLotId, generatedAt.atOffset(ZoneOffset.UTC),
                generatedAt.plus(15, ChronoUnit.MINUTES).atOffset(ZoneOffset.UTC), runId,
                runSlot.atOffset(ZoneOffset.UTC), trigger);
    }
}

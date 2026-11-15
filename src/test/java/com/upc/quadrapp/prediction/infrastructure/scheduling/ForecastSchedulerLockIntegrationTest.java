package com.upc.quadrapp.prediction.infrastructure.scheduling;

import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The forecast runs (every 5 minutes) and the daily purge run in one replica at a time: the
 * replica that takes the ShedLock lock runs the job, the others skip it.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class ForecastSchedulerLockIntegrationTest {

    @Autowired
    private ForecastScheduler scheduler;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ForecastCommandService forecasts;

    @Test
    void runsAreScheduledEveryFiveMinutesAndThePurgeDaily() throws Exception {
        assertThat(ForecastScheduler.class.getMethod("generateForecasts").getAnnotation(Scheduled.class).cron())
                .isEqualTo("0 */5 * * * *");
        assertThat(ForecastScheduler.class.getMethod("purgeExpiredForecasts").getAnnotation(Scheduled.class).cron())
                .isEqualTo("0 23 4 * * *");
    }

    @Test
    void freeLockRunsTheForecastsAndHeldLockSkipsThem() {
        holdLock("forecast-runs", "previous-replica", "- interval '1 second'");
        clearInvocations(forecasts);
        scheduler.generateForecasts();
        verify(forecasts).generateScheduledRuns();

        holdLock("forecast-runs", "other-replica", "+ interval '1 hour'");
        clearInvocations(forecasts);
        scheduler.generateForecasts();
        verify(forecasts, never()).generateScheduledRuns();
    }

    @Test
    void freeLockRunsThePurgeAndHeldLockSkipsIt() {
        holdLock("forecast-purge", "previous-replica", "- interval '1 second'");
        clearInvocations(forecasts);
        scheduler.purgeExpiredForecasts();
        verify(forecasts).purgeExpiredForecasts();

        holdLock("forecast-purge", "other-replica", "+ interval '1 hour'");
        clearInvocations(forecasts);
        scheduler.purgeExpiredForecasts();
        verify(forecasts, never()).purgeExpiredForecasts();
    }

    /** ShedLock compares {@code lock_until} with the database clock in UTC. */
    private void holdLock(String lock, String replica, String untilOffset) {
        jdbc.update("insert into shared.shedlock (name, lock_until, locked_at, locked_by) "
                + "values (?, timezone('utc', now()) " + untilOffset + ", timezone('utc', now()), ?) "
                + "on conflict (name) do update set lock_until = excluded.lock_until, "
                + "locked_at = excluded.locked_at, locked_by = excluded.locked_by", lock, replica);
    }
}

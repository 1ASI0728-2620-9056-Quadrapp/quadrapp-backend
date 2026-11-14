package com.upc.quadrapp.prediction.infrastructure.scheduling;

import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * With several backend replicas, the forecast evaluation runs in only one of them at a time:
 * the replica that takes the lock in the shared database runs it, the others skip it.
 */
@QuadrappIntegrationTest
class ForecastEvaluationSchedulerLockIntegrationTest {

    private static final String LOCK = "forecast-evaluation";

    @Autowired
    private ForecastEvaluationScheduler scheduler;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ForecastCommandService forecasts;

    @Test
    void freeLockRunsTheEvaluationAndRecordsTheLock() {
        holdLock("previous-replica", "- interval '1 second'");
        clearInvocations(forecasts);

        scheduler.evaluateDueForecasts();

        verify(forecasts).evaluateDueForecasts();
        String lockedBy = jdbc.queryForObject(
                "select locked_by from shared.shedlock where name = ?", String.class, LOCK);
        assertThat(lockedBy).isNotEqualTo("previous-replica");
    }

    @Test
    void lockHeldByAnotherReplicaSkipsTheEvaluation() {
        holdLock("other-replica", "+ interval '1 hour'");
        clearInvocations(forecasts);

        scheduler.evaluateDueForecasts();

        verify(forecasts, never()).evaluateDueForecasts();
    }

    /** ShedLock compares {@code lock_until} with the database clock in UTC. */
    private void holdLock(String replica, String untilOffset) {
        jdbc.update("insert into shared.shedlock (name, lock_until, locked_at, locked_by) "
                + "values (?, timezone('utc', now()) " + untilOffset + ", timezone('utc', now()), ?) "
                + "on conflict (name) do update set lock_until = excluded.lock_until, "
                + "locked_at = excluded.locked_at, locked_by = excluded.locked_by", LOCK, replica);
    }
}

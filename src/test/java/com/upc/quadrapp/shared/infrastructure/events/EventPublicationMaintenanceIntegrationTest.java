package com.upc.quadrapp.shared.infrastructure.events;

import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingsensing.domain.model.events.SensorStateChanged;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A publication whose listener failed is delivered again by a periodic job, not only on restart,
 * up to a maximum number of attempts, including publications a stopped replica left incomplete
 * (nothing is republished without limit on restart); completed publications are deleted after a
 * retention period, so the registry does not grow forever.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class EventPublicationMaintenanceIntegrationTest {

    @Autowired
    private EventPublicationMaintenance maintenance;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Environment environment;

    @MockitoBean
    private OccupancyCommandService occupancy;

    /** Each test calls the jobs directly, so the ShedLock rows of earlier calls are released. */
    @BeforeEach
    void clearRegistryAndLocks() {
        jdbc.update("delete from shared.event_publication");
        jdbc.update("update shared.shedlock set lock_until = timezone('utc', now()) - interval '1 second' "
                + "where name like 'event-publication-%'");
    }

    @Test
    void everyJobIsScheduledAndLocked() {
        Method[] jobs = Arrays.stream(EventPublicationMaintenance.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .toArray(Method[]::new);

        assertThat(jobs).hasSize(2).allSatisfy(job ->
                assertThat(job.isAnnotationPresent(SchedulerLock.class)).as(job.getName()).isTrue());
    }

    @Test
    void aFailedPublicationIsDeliveredAgainByTheRetryJob() {
        when(occupancy.handle(any(RecordSensorEventCommand.class)))
                .thenThrow(new IllegalStateException("Occupancy is unavailable."))
                .thenReturn(true);
        publishDetection();
        awaitStatus("FAILED");

        maintenance.retryFailedPublications();

        verify(occupancy, timeout(5_000).times(2)).handle(any(RecordSensorEventCommand.class));
        awaitStatus("COMPLETED");
    }

    @Test
    void incompletePublicationsAreNotRepublishedWithoutLimitOnRestart() {
        assertThat(environment.getProperty("spring.modulith.events.republish-outstanding-events-on-restart",
                Boolean.class)).isFalse();
    }

    /**
     * A replica that stops while a publication is waiting or in progress leaves it incomplete; once
     * it is older than the staleness limit it becomes FAILED and the retry job delivers it again.
     */
    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "PROCESSING", "RESUBMITTED"})
    void aPublicationLeftIncompleteByAStoppedReplicaIsRetried(String leftStatus) {
        when(occupancy.handle(any(RecordSensorEventCommand.class)))
                .thenThrow(new IllegalStateException("Occupancy is unavailable."))
                .thenReturn(true);
        publishDetection();
        awaitStatus("FAILED");
        jdbc.update("update shared.event_publication set status = ?, "
                + "publication_date = now() - interval '20 minutes', "
                + "last_resubmission_date = now() - interval '20 minutes'", leftStatus);

        maintenance.retryFailedPublications();

        verify(occupancy, timeout(5_000).times(2)).handle(any(RecordSensorEventCommand.class));
        awaitStatus("COMPLETED");
    }

    @Test
    void aPublicationThatReachedTheMaximumAttemptsIsNotRetried() {
        when(occupancy.handle(any(RecordSensorEventCommand.class)))
                .thenThrow(new IllegalStateException("Occupancy is unavailable."));
        publishDetection();
        awaitStatus("FAILED");
        jdbc.update("update shared.event_publication set completion_attempts = 5");

        maintenance.retryFailedPublications();

        verify(occupancy, after(1_000).times(1)).handle(any(RecordSensorEventCommand.class));
        assertThat(status()).isEqualTo("FAILED");
    }

    @Test
    void completedPublicationsOlderThanTheRetentionAreDeleted() {
        Instant now = Instant.now();
        String old = completed(now.minus(Duration.ofDays(8)));
        String recent = completed(now.minus(Duration.ofDays(6)));

        maintenance.deleteCompletedPublications();

        assertThat(jdbc.queryForList("select cast(id as text) from shared.event_publication", String.class))
                .containsExactly(recent).doesNotContain(old);
    }

    private void publishDetection() {
        transactions.executeWithoutResult(status -> events.publishEvent(new SensorStateChanged(
                UUID.randomUUID().toString(), 40L, 10L, 20L, 30L, SensorStateChanged.State.OCCUPIED,
                Instant.parse("2026-11-14T10:00:00Z"))));
    }

    private String completed(Instant completedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into shared.event_publication (id, listener_id, event_type, serialized_event, "
                + "publication_date, completion_date, status, completion_attempts) "
                + "values (?, 'listener', 'type', '{}', ?, ?, 'COMPLETED', 1)",
                id, Timestamp.from(completedAt), Timestamp.from(completedAt));
        return id.toString();
    }

    private String status() {
        return jdbc.queryForObject("select status from shared.event_publication", String.class);
    }

    private void awaitStatus(String expected) {
        long deadline = System.currentTimeMillis() + 5_000;
        String current = null;
        while (System.currentTimeMillis() < deadline) {
            current = status();
            if (expected.equals(current)) {
                return;
            }
            LockSupport.parkNanos(Duration.ofMillis(50).toNanos());
        }
        fail("Publication status was " + current + " instead of " + expected);
    }
}

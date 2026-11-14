package com.upc.quadrapp.parkingsensing.infrastructure.events;

import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events.SensingEventPublisherPort;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Domain events of Parking Sensing are stored in the event publication registry inside the
 * transaction that produced them (transactional outbox), so a crash after the commit
 * cannot lose them and a rollback never publishes them (QAS-03). Occupancy is replaced by a
 * mock to observe what its listener receives.
 */
@QuadrappIntegrationTest
class SensingEventOutboxIntegrationTest {

    private static final Instant DETECTED_AT = Instant.parse("2026-01-01T10:05:00Z");

    @Autowired
    private SensingEventPublisherPort publisher;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IncompleteEventPublications incompletePublications;

    @MockitoBean
    private OccupancyCommandService occupancy;

    @BeforeEach
    void clearRegistry() {
        jdbc.update("delete from shared.event_publication");
    }

    @Test
    void committedDetectionsAreStoredForTheOccupancyListener() {
        transactions.executeWithoutResult(status -> {
            publisher.publishSpaceDetection(10L, 20L, 30L, 40L, "evt-1", SensorState.OCCUPIED, DETECTED_AT);
            publisher.publishPassageDetection(10L, 20L, 50L, 41L, "evt-2", PassDirection.IN, DETECTED_AT);
        });

        List<String> stored = jdbc.queryForList(
                "select event_type || ' -> ' || listener_id from shared.event_publication order by event_type",
                String.class);
        assertThat(stored).hasSize(2);
        assertThat(stored.get(0)).startsWith(
                "com.upc.quadrapp.parkingsensing.domain.model.events.SensorStateChanged -> "
                        + "com.upc.quadrapp.occupancy.interfaces.events.SensingEventListener.on(");
        assertThat(stored.get(1)).startsWith(
                "com.upc.quadrapp.parkingsensing.domain.model.events.VehiclePassageDetected -> "
                        + "com.upc.quadrapp.occupancy.interfaces.events.SensingEventListener.on(");
    }

    @Test
    void rolledBackDetectionsAreNeverPublished() {
        transactions.executeWithoutResult(status -> {
            publisher.publishSpaceDetection(10L, 20L, 30L, 40L, "evt-3", SensorState.FREE, DETECTED_AT);
            status.setRollbackOnly();
        });

        Integer stored = jdbc.queryForObject("select count(*) from shared.event_publication", Integer.class);
        assertThat(stored).isZero();
    }

    @Test
    void failedDeliveryIsPublishedAgainWithTheSameEvent() {
        Instant detectedAt = Instant.parse("2026-01-01T10:05:00.123456Z");
        RecordSensorEventCommand expected = new RecordSensorEventCommand(
                10L, 20L, 40L, 30L, null, "evt-4", OccupancyEventType.OCCUPIED, detectedAt);
        when(occupancy.handle(any(RecordSensorEventCommand.class)))
                .thenThrow(new IllegalStateException("Occupancy is unavailable."))
                .thenReturn(true);

        transactions.executeWithoutResult(status ->
                publisher.publishSpaceDetection(10L, 20L, 30L, 40L, "evt-4", SensorState.OCCUPIED, detectedAt));
        verify(occupancy, timeout(5_000)).handle(expected);
        awaitPublicationStatus("FAILED");

        incompletePublications.resubmitIncompletePublications(publication -> true);

        verify(occupancy, timeout(5_000).times(2)).handle(expected);
        awaitPublicationStatus("COMPLETED");
    }

    private void awaitPublicationStatus(String expected) {
        long deadline = System.currentTimeMillis() + 5_000;
        String status = null;
        while (System.currentTimeMillis() < deadline) {
            status = jdbc.queryForObject("select status from shared.event_publication", String.class);
            if (expected.equals(status)) {
                return;
            }
            LockSupport.parkNanos(Duration.ofMillis(50).toNanos());
        }
        fail("Publication status was " + status + ", expected " + expected);
    }
}

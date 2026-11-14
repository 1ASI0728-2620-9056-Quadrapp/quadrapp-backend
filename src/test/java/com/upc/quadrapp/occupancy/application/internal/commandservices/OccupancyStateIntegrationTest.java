package com.upc.quadrapp.occupancy.application.internal.commandservices;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.events.LowAvailabilityEnded;
import com.upc.quadrapp.occupancy.domain.model.events.LowAvailabilityStarted;
import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedOccupied;
import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedUnknown;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Occupancy owns the state of every space and parking lot in its own schema. Detections from
 * Parking Sensing change only that state, never the configuration tables, and events that can
 * never apply (unknown lot, space of another lot, another tenant) are dropped instead of failing
 * forever.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
@RecordApplicationEvents
class OccupancyStateIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-11-14T10:00:00Z");

    @Autowired
    private OccupancyCommandService commands;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEvents events;

    private OccupancyTestLayout layout;

    @BeforeEach
    void layoutWithTenSpaces() {
        layout = OccupancyTestLayout.create(jdbc, 10, 10);
    }

    @Test
    void aDetectionChangesTheOccupancyStateAndNeverTheConfiguration() {
        assertThat(commands.handle(space(layout.space(0), OccupancyEventType.OCCUPIED, T0))).isTrue();

        assertThat(spaceStatus(layout.space(0))).isEqualTo("OCCUPIED");
        assertThat(spaceStatus(layout.space(1))).isEqualTo("UNKNOWN");
        assertThat(jdbc.queryForObject("select status from configuration.spaces where id = ?", String.class,
                layout.space(0))).isEqualTo("UNKNOWN");
        assertThat(lotCounts()).containsEntry("total_spaces", 10).containsEntry("occupied_count", 1)
                .containsEntry("free_count", 0).containsEntry("unknown_count", 9);
    }

    @Test
    void theOccupancyEventsArePublishedWithTheirTenant() {
        commands.handle(space(layout.space(0), OccupancyEventType.OCCUPIED, T0));

        assertThat(events.stream(SpaceMarkedUnknown.class).filter(e -> e.parkingLotId() == layout.parkingLotId))
                .hasSize(10)
                .allSatisfy(event -> {
                    assertThat(event.tenantId()).isEqualTo(layout.institutionId);
                    assertThat(event.zoneId()).isEqualTo(layout.zoneId);
                    assertThat(event.reason()).isEqualTo(SpaceMarkedUnknown.Reason.NOT_YET_REPORTED);
                });
        assertThat(events.stream(SpaceMarkedOccupied.class))
                .containsExactly(new SpaceMarkedOccupied(layout.space(0), layout.parkingLotId, layout.zoneId, T0));
    }

    @Test
    void anInactiveSpaceNeverChangesStateAndIsNotCounted() {
        commands.handle(space(layout.space(0), OccupancyEventType.FREED, T0));

        commands.handle(space(layout.inactiveSpaceId, OccupancyEventType.OCCUPIED, T0.plusSeconds(1)));

        assertThat(spaceStatus(layout.inactiveSpaceId)).isEqualTo("UNKNOWN");
        assertThat(lotCounts()).containsEntry("total_spaces", 10).containsEntry("occupied_count", 0);
    }

    @Test
    void aDetectionOlderThanTheLastAppliedOneIsIgnored() {
        commands.handle(space(layout.space(0), OccupancyEventType.FREED, T0.plusSeconds(10)));

        assertThat(commands.handle(space(layout.space(0), OccupancyEventType.OCCUPIED, T0))).isTrue();

        assertThat(spaceStatus(layout.space(0))).isEqualTo("FREE");
    }

    @Test
    void anEventIdIsAppliedOnlyOnce() {
        RecordSensorEventCommand command = space(layout.space(0), OccupancyEventType.OCCUPIED, T0);

        assertThat(commands.handle(command)).isTrue();
        assertThat(commands.handle(command)).isFalse();
    }

    @Test
    void eventsThatCanNeverApplyAreDroppedWithoutFailing() {
        OccupancyTestLayout other = OccupancyTestLayout.create(jdbc, 1, 10);
        commands.handle(space(layout.space(0), OccupancyEventType.FREED, T0));

        assertThatCode(() -> commands.handle(new RecordSensorEventCommand(layout.institutionId, Long.MAX_VALUE, 1L,
                layout.space(0), null, eventId(), OccupancyEventType.OCCUPIED, T0.plusSeconds(1))))
                .doesNotThrowAnyException();
        assertThatCode(() -> commands.handle(space(other.space(0), OccupancyEventType.OCCUPIED, T0.plusSeconds(2))))
                .doesNotThrowAnyException();
        assertThatCode(() -> commands.handle(new RecordSensorEventCommand(other.institutionId, layout.parkingLotId,
                1L, layout.space(0), null, eventId(), OccupancyEventType.OCCUPIED, T0.plusSeconds(3))))
                .doesNotThrowAnyException();
        assertThatCode(() -> commands.handle(new RecordSensorEventCommand(layout.institutionId, layout.parkingLotId,
                1L, null, Long.MAX_VALUE, eventId(), OccupancyEventType.ENTRY, T0.plusSeconds(4))))
                .doesNotThrowAnyException();

        assertThat(spaceStatus(layout.space(0))).isEqualTo("FREE");
        assertThat(jdbc.queryForObject("select count(*) from occupancy.space_occupancy where space_id = ?",
                Integer.class, other.space(0))).isZero();
    }

    @Test
    void aPassageThroughAnAccessOfTheLotIsRecordedOnce() {
        RecordSensorEventCommand entry = new RecordSensorEventCommand(layout.institutionId, layout.parkingLotId, 1L,
                null, layout.accessId, eventId(), OccupancyEventType.ENTRY, T0);

        commands.handle(entry);
        commands.handle(entry);

        assertThat(jdbc.queryForList("select direction from occupancy.vehicle_passages where parking_lot_id = ?",
                String.class, layout.parkingLotId)).containsExactly("IN");
    }

    @Test
    void lowAvailabilityStartsBelowTheThresholdAndEndsAboveItPlusFivePoints() {
        for (int i = 0; i < 10; i++) {
            commands.handle(space(layout.space(i), OccupancyEventType.FREED, T0));
        }
        for (int i = 0; i < 9; i++) {
            commands.handle(space(layout.space(i), OccupancyEventType.OCCUPIED, T0.plusSeconds(1)));
        }
        assertThat(lowAvailability()).isFalse();

        commands.handle(space(layout.space(9), OccupancyEventType.OCCUPIED, T0.plusSeconds(1)));
        assertThat(lowAvailability()).isTrue();

        commands.handle(space(layout.space(0), OccupancyEventType.FREED, T0.plusSeconds(2)));
        assertThat(lowAvailability()).isTrue();

        commands.handle(space(layout.space(1), OccupancyEventType.FREED, T0.plusSeconds(2)));
        assertThat(lowAvailability()).isFalse();

        assertThat(events.stream(LowAvailabilityStarted.class)).singleElement()
                .isEqualTo(started(0));
        assertThat(events.stream(LowAvailabilityEnded.class)).singleElement()
                .satisfies(ended -> {
                    assertThat(ended.tenantId()).isEqualTo(layout.institutionId);
                    assertThat(ended.freeSpaces()).isEqualTo(2);
                    assertThat(ended.capacity()).isEqualTo(10);
                });
    }

    private LowAvailabilityStarted started(long free) {
        LowAvailabilityStarted event = events.stream(LowAvailabilityStarted.class).findFirst().orElseThrow();
        return new LowAvailabilityStarted(layout.institutionId, layout.parkingLotId, free, 10, event.at());
    }

    @Test
    void theSameEventDeliveredByTwoReplicasAtOnceIsAppliedOnceAndNeitherFails() throws Exception {
        RecordSensorEventCommand command = space(layout.space(0), OccupancyEventType.OCCUPIED, T0);

        List<Boolean> results = concurrently(() -> commands.handle(command), () -> commands.handle(command));

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(jdbc.queryForObject("select count(*) from occupancy.sensor_events where event_id = ?",
                Integer.class, command.eventId())).isEqualTo(1);
        assertThat(spaceStatus(layout.space(0))).isEqualTo("OCCUPIED");
    }

    @Test
    void theFirstEventsOfAParkingLotArrivingAtOnceDoNotFail() throws Exception {
        List<Boolean> results = concurrently(
                () -> commands.handle(space(layout.space(0), OccupancyEventType.OCCUPIED, T0)),
                () -> commands.handle(space(layout.space(1), OccupancyEventType.FREED, T0)));

        assertThat(results).containsExactly(true, true);
        assertThat(lotCounts()).containsEntry("occupied_count", 1).containsEntry("free_count", 1);
    }

    private List<Boolean> concurrently(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private RecordSensorEventCommand space(long spaceId, OccupancyEventType type, Instant at) {
        return new RecordSensorEventCommand(layout.institutionId, layout.parkingLotId, 1L, spaceId, null, eventId(),
                type, at);
    }

    private static String eventId() {
        return UUID.randomUUID().toString();
    }

    private String spaceStatus(long spaceId) {
        return jdbc.queryForObject("select status from occupancy.space_occupancy where space_id = ?", String.class,
                spaceId);
    }

    private Map<String, Object> lotCounts() {
        return jdbc.queryForMap("select total_spaces, free_count, occupied_count, unknown_count "
                + "from occupancy.lot_occupancy where parking_lot_id = ?", layout.parkingLotId);
    }

    private boolean lowAvailability() {
        return jdbc.queryForObject("select low_availability from occupancy.lot_occupancy where parking_lot_id = ?",
                Boolean.class, layout.parkingLotId);
    }
}

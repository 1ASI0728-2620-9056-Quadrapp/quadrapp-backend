package com.upc.quadrapp.occupancy.interfaces.scheduling;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Occupancy keeps one snapshot per zone and minute for 8 weeks (the window of the Prediction
 * fallback) and vehicle passages for 7 days; older rows are purged by a scheduled job that runs
 * in one replica at a time.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class OccupancySchedulersIntegrationTest {

    @Autowired
    private OccupancySchedulers schedulers;

    @Autowired
    private OccupancyCommandService commands;

    @Autowired
    private JdbcTemplate jdbc;

    /** Each test calls the jobs directly, so the ShedLock rows of earlier calls are released. */
    @BeforeEach
    void releaseLocks() {
        jdbc.update("update shared.shedlock set lock_until = timezone('utc', now()) - interval '1 second' "
                + "where name like 'occupancy-%'");
    }

    @Test
    void everyJobIsScheduledAndLocked() {
        Method[] jobs = Arrays.stream(OccupancySchedulers.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .toArray(Method[]::new);

        assertThat(jobs).hasSize(2).allSatisfy(job ->
                assertThat(job.isAnnotationPresent(SchedulerLock.class)).as(job.getName()).isTrue());
    }

    @Test
    void eachMinuteSavesOneSnapshotPerZone() {
        OccupancyTestLayout layout = OccupancyTestLayout.create(jdbc, 3, 10);
        commands.handle(new RecordSensorEventCommand(layout.institutionId, layout.parkingLotId, 1L, layout.space(0),
                null, UUID.randomUUID().toString(), OccupancyEventType.OCCUPIED, Instant.now()));

        schedulers.recordSnapshots();
        releaseLocks();
        schedulers.recordSnapshots();

        var rows = jdbc.queryForList("select taken_at, total_spaces, occupied_count, unknown_count "
                + "from occupancy.occupancy_snapshots where zone_id = ? and parking_lot_id = ?",
                layout.zoneId, layout.parkingLotId);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("total_spaces", 3)
                .containsEntry("occupied_count", 1)
                .containsEntry("unknown_count", 2);
        Instant takenAt = ((Timestamp) rows.getFirst().get("taken_at")).toInstant();
        assertThat(takenAt).isEqualTo(takenAt.truncatedTo(ChronoUnit.MINUTES));
    }

    @Test
    void snapshotsOlderThanEightWeeksAndPassagesOlderThanSevenDaysArePurged() {
        long lot = Long.MAX_VALUE - Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000);
        Instant now = Instant.now();
        snapshot(lot, 1L, now.minus(Duration.ofDays(57)));
        snapshot(lot, 1L, now.minus(Duration.ofDays(55)));
        String oldPassage = passage(lot, now.minus(Duration.ofDays(8)));
        String recentPassage = passage(lot, now.minus(Duration.ofDays(6)));
        String oldEvent = sensorEvent(now.minus(Duration.ofDays(8)));
        String recentEvent = sensorEvent(now.minus(Duration.ofDays(6)));

        schedulers.purgeExpiredHistory();

        assertThat(jdbc.queryForList("select taken_at from occupancy.occupancy_snapshots where parking_lot_id = ?",
                Timestamp.class, lot)).hasSize(1).allSatisfy(takenAt ->
                assertThat(takenAt.toInstant()).isAfter(now.minus(Duration.ofDays(56))));
        assertThat(jdbc.queryForList("select event_id from occupancy.vehicle_passages where parking_lot_id = ?",
                String.class, lot)).containsExactly(recentPassage).doesNotContain(oldPassage);
        assertThat(jdbc.queryForList("select event_id from occupancy.sensor_events where event_id in (?, ?)",
                String.class, oldEvent, recentEvent)).containsExactly(recentEvent);
    }

    private void snapshot(long lot, long zone, Instant takenAt) {
        jdbc.update("insert into occupancy.occupancy_snapshots (parking_lot_id, zone_id, taken_at, total_spaces, "
                + "free_count, occupied_count, unknown_count) values (?, ?, ?, 1, 1, 0, 0)",
                lot, lot - zone, Timestamp.from(takenAt));
    }

    private String passage(long lot, Instant passedAt) {
        String eventId = UUID.randomUUID().toString();
        jdbc.update("insert into occupancy.vehicle_passages (event_id, tenant_id, parking_lot_id, access_point_id, "
                + "direction, passed_at, recorded_at) values (?, 1, ?, 1, 'IN', ?, ?)",
                eventId, lot, Timestamp.from(passedAt), Timestamp.from(passedAt));
        return eventId;
    }

    private String sensorEvent(Instant receivedAt) {
        String eventId = UUID.randomUUID().toString();
        jdbc.update("insert into occupancy.sensor_events (event_id, device_id, space_id, event_type, occurred_at, "
                + "received_at) values (?, 1, 1, 'OCCUPIED', ?, ?)",
                eventId, Timestamp.from(receivedAt), Timestamp.from(receivedAt));
        return eventId;
    }
}

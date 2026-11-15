package com.upc.quadrapp.occupancy.interfaces.acl;

import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prediction reads the history of a parking lot only through this facade: the lot totals of the
 * minute snapshot closest to a moment (to evaluate a forecast against its target time) and the
 * average occupancy of the same weekday and 15-minute slot over past weeks (fallback forecast).
 * Occupancy is occupied / (free + occupied): unknown spaces never count.
 */
@QuadrappIntegrationTest
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class OccupancyHistoryFacadeIntegrationTest {

    private static final Instant SLOT = Instant.parse("2026-11-12T13:00:00Z");

    @Autowired
    private OccupancyContextFacade occupancy;

    @Autowired
    private JdbcTemplate jdbc;

    private long lot;
    private long zoneA;
    private long zoneB;

    @BeforeEach
    void ids() {
        lot = ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000);
        zoneA = lot * 10 + 1;
        zoneB = lot * 10 + 2;
    }

    @Test
    void lotOccupancyAtAddsTheZonesOfTheClosestSnapshotWithinTheTolerance() {
        Instant target = SLOT.plusSeconds(30);
        snapshot(zoneA, target.minusSeconds(90), 10, 0, 0);
        snapshot(zoneA, target.plusSeconds(20), 4, 5, 1);
        snapshot(zoneB, target.plusSeconds(20), 2, 3, 5);
        snapshot(zoneA, target.plus(20, ChronoUnit.MINUTES), 0, 10, 0);

        var view = occupancy.lotOccupancyAt(lot, target, Duration.ofMinutes(5)).orElseThrow();

        assertThat(view.takenAt()).isEqualTo(target.plusSeconds(20));
        assertThat(view.totalSpaces()).isEqualTo(20);
        assertThat(view.freeCount()).isEqualTo(6);
        assertThat(view.occupiedCount()).isEqualTo(8);
        assertThat(view.unknownCount()).isEqualTo(6);
        assertThat(occupancy.lotOccupancyAt(lot, target.plus(10, ChronoUnit.MINUTES), Duration.ofMinutes(5)))
                .isEmpty();
    }

    @Test
    void weeklySlotAverageUsesTheSameSlotOfEachPastWeekOnly() {
        // Week 1: 40 % and 60 % in two minutes of the slot; week 3: 80 %; week 2 has no data.
        snapshot(zoneA, SLOT.minus(7, ChronoUnit.DAYS).plusSeconds(65), 6, 4, 0);
        snapshot(zoneA, SLOT.minus(7, ChronoUnit.DAYS).plusSeconds(125), 4, 6, 0);
        snapshot(zoneA, SLOT.minus(21, ChronoUnit.DAYS).plusSeconds(65), 2, 8, 5);
        // Ignored: all spaces unknown, just outside the slot, and more than 8 weeks back.
        snapshot(zoneA, SLOT.minus(14, ChronoUnit.DAYS).plusSeconds(65), 0, 0, 10);
        snapshot(zoneA, SLOT.minus(7, ChronoUnit.DAYS).plus(15, ChronoUnit.MINUTES), 0, 10, 0);
        snapshot(zoneA, SLOT.minus(63, ChronoUnit.DAYS).plusSeconds(65), 0, 10, 0);
        // The current week is not history.
        snapshot(zoneA, SLOT.plusSeconds(65), 0, 10, 0);

        var average = occupancy.weeklySlotAverage(lot, SLOT, Duration.ofMinutes(15), 8);

        assertThat(average.weeksWithData()).isEqualTo(2);
        assertThat(average.averageOccupancyPct()).isEqualByComparingTo("60.00");
    }

    @Test
    void weeklySlotAverageWithoutHistoryIsEmpty() {
        var average = occupancy.weeklySlotAverage(lot, SLOT, Duration.ofMinutes(15), 8);

        assertThat(average.weeksWithData()).isZero();
        assertThat(average.averageOccupancyPct()).isNull();
    }

    private void snapshot(long zoneId, Instant takenAt, int free, int occupied, int unknown) {
        jdbc.update("insert into occupancy.occupancy_snapshots (parking_lot_id, zone_id, taken_at, total_spaces, "
                        + "free_count, occupied_count, unknown_count) values (?, ?, ?, ?, ?, ?, ?)",
                lot, zoneId, takenAt.atOffset(ZoneOffset.UTC), free + occupied + unknown, free, occupied, unknown);
    }
}

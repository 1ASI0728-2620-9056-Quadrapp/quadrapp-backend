package com.upc.quadrapp.occupancy.domain.model.aggregates;

import com.upc.quadrapp.occupancy.domain.model.events.LowAvailabilityEnded;
import com.upc.quadrapp.occupancy.domain.model.events.LowAvailabilityStarted;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.ZoneCount;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Low availability starts when the free share of the known spaces (free over free plus occupied)
 * falls below the lot's threshold and ends only when it rises above the threshold plus 5 points
 * (report, 5.4.1 and rule 8 of Notifications). UNKNOWN spaces never count either way.
 */
class LotOccupancyTest {

    private static final Instant AT = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void startsBelowTheThresholdAndEndsAboveItPlusFivePoints() {
        LotOccupancy lot = lot(10);

        recalculate(lot, 10);
        assertThat(lot.pullDomainEvents()).isEmpty();

        recalculate(lot, 9);
        assertThat(lot.isLowAvailability()).isTrue();
        assertThat(lot.pullDomainEvents()).containsExactly(new LowAvailabilityStarted(3L, 7L, 9, 100, AT));

        recalculate(lot, 15);
        assertThat(lot.isLowAvailability()).isTrue();
        assertThat(lot.pullDomainEvents()).isEmpty();

        recalculate(lot, 16);
        assertThat(lot.isLowAvailability()).isFalse();
        assertThat(lot.pullDomainEvents()).containsExactly(new LowAvailabilityEnded(3L, 7L, 16, 100, AT));
    }

    @Test
    void exactlyAtTheThresholdIsNotLowAvailability() {
        LotOccupancy lot = lot(10);

        recalculate(lot, 10);

        assertThat(lot.isLowAvailability()).isFalse();
    }

    @Test
    void unknownSpacesDoNotCountAsCapacityForLowAvailability() {
        LotOccupancy lot = lot(10);

        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(200, 1, 0, 199))), AT);

        assertThat(lot.isLowAvailability()).isFalse();
        assertThat(lot.pullDomainEvents()).isEmpty();
    }

    @Test
    void theRatioIsFreeOverKnownSpaces() {
        LotOccupancy lot = lot(10);

        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(100, 1, 9, 90))), AT);
        assertThat(lot.isLowAvailability()).isFalse();

        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(100, 1, 10, 89))), AT);
        assertThat(lot.isLowAvailability()).isTrue();
        assertThat(lot.pullDomainEvents()).containsExactly(new LowAvailabilityStarted(3L, 7L, 1, 100, AT));

        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(100, 3, 17, 80))), AT);
        assertThat(lot.isLowAvailability()).isTrue();

        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(100, 4, 17, 79))), AT);
        assertThat(lot.isLowAvailability()).isFalse();
        assertThat(lot.pullDomainEvents()).containsExactly(new LowAvailabilityEnded(3L, 7L, 4, 100, AT));
    }

    @Test
    void theThresholdOfTheLotIsUsed() {
        LotOccupancy lot = lot(30);

        recalculate(lot, 29);

        assertThat(lot.isLowAvailability()).isTrue();
    }

    @Test
    void withoutAnyKnownSpaceTheConditionDoesNotChange() {
        LotOccupancy lot = lot(10);

        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(100, 0, 0, 100))), AT);

        assertThat(lot.isLowAvailability()).isFalse();
        assertThat(lot.pullDomainEvents()).isEmpty();
        assertThat(lot.getCounts().unknownCount()).isEqualTo(100);
    }

    @Test
    void theCountsAreTheSumOfTheZones() {
        LotOccupancy lot = lot(10);

        lot.recalculate(List.of(
                new ZoneCount(1L, new OccupancyCounts(10, 4, 5, 1)),
                new ZoneCount(2L, new OccupancyCounts(20, 6, 10, 4))), AT);

        assertThat(lot.getCounts()).isEqualTo(new OccupancyCounts(30, 10, 15, 5));
        assertThat(lot.getConsolidatedAt()).isEqualTo(AT);
    }

    private static LotOccupancy lot(int threshold) {
        return LotOccupancy.restore(7L, 3L, new OccupancyCounts(0, 0, 0, 0), threshold, false, null, AT, 0L);
    }

    private static void recalculate(LotOccupancy lot, long free) {
        lot.recalculate(List.of(new ZoneCount(1L, new OccupancyCounts(100, free, 100 - free, 0))), AT);
    }
}

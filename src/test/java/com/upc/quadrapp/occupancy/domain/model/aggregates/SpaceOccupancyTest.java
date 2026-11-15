package com.upc.quadrapp.occupancy.domain.model.aggregates;

import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedFree;
import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedOccupied;
import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedUnknown;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.UnknownReason;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Rules of the space state (report 5.4.1, rules 1 to 5). */
class SpaceOccupancyTest {

    private static final Instant T0 = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void aNewSpaceStartsUnknownAndPublishesItWithItsTenant() {
        SpaceOccupancy space = SpaceOccupancy.initialize(11L, 3L, 7L, 5L, 6L, true, T0);

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.UNKNOWN);
        assertThat(space.getUnknownReason()).isEqualTo(UnknownReason.NOT_YET_REPORTED);
        assertThat(space.countsAsFree()).isFalse();
        assertThat(space.pullDomainEvents()).containsExactly(new SpaceMarkedUnknown(
                3L, 11L, 7L, 5L, SpaceMarkedUnknown.Reason.NOT_YET_REPORTED, T0));
    }

    @Test
    void aDetectionLeavesUnknownAndRegistersTheChange() {
        SpaceOccupancy space = known();

        assertThat(space.applyDetection("e1", SpaceStatus.OCCUPIED, T0.plusSeconds(1))).isTrue();

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.OCCUPIED);
        assertThat(space.getUnknownReason()).isNull();
        assertThat(space.pullDomainEvents()).containsExactly(new SpaceMarkedOccupied(11L, 7L, 5L, T0.plusSeconds(1)));

        space.applyDetection("e2", SpaceStatus.FREE, T0.plusSeconds(2));
        assertThat(space.pullDomainEvents()).containsExactly(new SpaceMarkedFree(11L, 7L, 5L, T0.plusSeconds(2)));
    }

    @Test
    void anAppliedOrOlderDetectionChangesNothing() {
        SpaceOccupancy space = known();
        space.applyDetection("e1", SpaceStatus.FREE, T0.plusSeconds(10));
        space.pullDomainEvents();

        assertThat(space.applyDetection("e1", SpaceStatus.OCCUPIED, T0.plusSeconds(20))).isFalse();
        assertThat(space.applyDetection("e2", SpaceStatus.OCCUPIED, T0.plusSeconds(10))).isFalse();
        assertThat(space.applyDetection("e3", SpaceStatus.OCCUPIED, T0.plusSeconds(5))).isFalse();

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.FREE);
        assertThat(space.pullDomainEvents()).isEmpty();
    }

    @Test
    void anInactiveSpaceNeverChangesState() {
        SpaceOccupancy space = SpaceOccupancy.initialize(11L, 3L, 7L, 5L, 6L, false, T0);
        space.pullDomainEvents();

        assertThat(space.applyDetection("e1", SpaceStatus.OCCUPIED, T0.plusSeconds(1))).isFalse();

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.UNKNOWN);
        assertThat(space.pullDomainEvents()).isEmpty();
    }

    @Test
    void aSpaceBackInUseIsUnknownUntilItsNextDetection() {
        SpaceOccupancy space = known();
        space.applyDetection("e1", SpaceStatus.OCCUPIED, T0.plusSeconds(1));
        space.applyLayout(5L, 6L, false, T0.plusSeconds(2));
        space.pullDomainEvents();

        space.applyLayout(5L, 6L, true, T0.plusSeconds(3));

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.UNKNOWN);
        assertThat(space.pullDomainEvents()).containsExactly(new SpaceMarkedUnknown(
                3L, 11L, 7L, 5L, SpaceMarkedUnknown.Reason.NOT_YET_REPORTED, T0.plusSeconds(3)));
    }

    @Test
    void markUnknownKeepsTheReasonAndPublishesTheTenant() {
        SpaceOccupancy space = known();
        space.applyDetection("e1", SpaceStatus.FREE, T0.plusSeconds(1));
        space.pullDomainEvents();

        space.markUnknown(UnknownReason.GATEWAY_OFFLINE, T0.plusSeconds(2));

        assertThat(space.getUnknownReason()).isEqualTo(UnknownReason.GATEWAY_OFFLINE);
        assertThat(space.pullDomainEvents()).containsExactly(new SpaceMarkedUnknown(
                3L, 11L, 7L, 5L, SpaceMarkedUnknown.Reason.GATEWAY_OFFLINE, T0.plusSeconds(2)));
    }

    private static SpaceOccupancy known() {
        SpaceOccupancy space = SpaceOccupancy.initialize(11L, 3L, 7L, 5L, 6L, true, T0);
        space.pullDomainEvents();
        return space;
    }
}

package com.upc.quadrapp.occupancy.infrastructure.realtime;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort.SpaceChange;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort.ZoneOccupancyChange;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Subscribers receive a change of a quiet zone at once, but while a gateway replays its buffer
 * they receive at most the last state of each zone per second (DD-04). Changes can arrive out of
 * order (each one is sent after its own commit), so the zone never goes back to an older state.
 */
class ThrottledOccupancyBroadcasterTest {

    private static final Instant T0 = Instant.parse("2026-11-14T10:00:00Z");
    private static final String TOPIC = "/topic/parking-lots/7/occupancy";

    private final SimpMessageSendingOperations messaging = mock(SimpMessageSendingOperations.class);
    private final MutableClock clock = new MutableClock(T0);
    private final List<Runnable> scheduled = new ArrayList<>();
    private final List<Duration> delays = new ArrayList<>();
    private final ThrottledOccupancyBroadcaster broadcaster = new ThrottledOccupancyBroadcaster(
            messaging, clock, (task, delay) -> {
                scheduled.add(task);
                delays.add(delay);
            });

    @Test
    void theFirstChangeOfAZoneIsSentAtOnce() {
        broadcaster.publish(change(1L, 100L, 5));

        OccupancyRealtimeMessage message = sent(1).getFirst();
        assertThat(message.parkingLotId()).isEqualTo(7L);
        assertThat(message.zoneId()).isEqualTo(1L);
        assertThat(message.zone().freeCount()).isEqualTo(5);
        assertThat(message.spaces()).extracting(OccupancyRealtimeMessage.SpaceState::spaceId).containsExactly(100L);
        assertThat(scheduled).isEmpty();
    }

    @Test
    void aBurstInTheSameSecondSendsOnlyTheLastStateOfTheZone() {
        broadcaster.publish(change(1L, 100L, 9));
        for (int i = 1; i <= 10; i++) {
            clock.advance(Duration.ofMillis(50));
            broadcaster.publish(change(1L, 100L + (i % 3), 9 - i % 9));
        }
        broadcaster.publish(change(1L, 101L, 2));
        sent(1);
        assertThat(scheduled).hasSize(1);
        assertThat(delays.getFirst()).isEqualTo(Duration.ofMillis(950));

        clock.advance(Duration.ofMillis(500));
        scheduled.getFirst().run();

        OccupancyRealtimeMessage last = sent(2).get(1);
        assertThat(last.zone().freeCount()).isEqualTo(2);
        assertThat(last.spaces()).extracting(OccupancyRealtimeMessage.SpaceState::spaceId)
                .containsExactlyInAnyOrder(100L, 101L, 102L);
    }

    @Test
    void zonesAreThrottledIndependently() {
        broadcaster.publish(change(1L, 100L, 5));
        broadcaster.publish(change(2L, 200L, 4));

        assertThat(sent(2)).extracting(OccupancyRealtimeMessage::zoneId).containsExactly(1L, 2L);
        assertThat(scheduled).isEmpty();
    }

    @Test
    void aChangeAfterAQuietSecondIsSentAtOnceAgain() {
        broadcaster.publish(change(1L, 100L, 5));
        clock.advance(Duration.ofMillis(1000));

        broadcaster.publish(change(1L, 100L, 4));

        assertThat(sent(2).get(1).zone().freeCount()).isEqualTo(4);
        assertThat(scheduled).isEmpty();
    }

    @Test
    void anOlderChangeArrivingLateNeverReplacesTheNewerStateOfTheZone() {
        broadcaster.publish(change(1L, 100L, 9, 1));
        clock.advance(Duration.ofMillis(100));
        broadcaster.publish(change(1L, 100L, 3, 3));
        broadcaster.publish(change(1L, 100L, 5, 2));

        clock.advance(Duration.ofMillis(900));
        scheduled.getFirst().run();

        OccupancyRealtimeMessage flushed = sent(2).get(1);
        assertThat(flushed.zone().freeCount()).isEqualTo(3);
        assertThat(flushed.spaces()).singleElement().extracting(OccupancyRealtimeMessage.SpaceState::spaceId)
                .isEqualTo(100L);
    }

    @Test
    void anOlderChangeOfTheSameSpaceAfterTheNewerWasSentIsDropped() {
        broadcaster.publish(change(1L, 100L, 3, 5));
        clock.advance(Duration.ofSeconds(2));

        broadcaster.publish(change(1L, 100L, 5, 4));

        sent(1);
        assertThat(scheduled).isEmpty();
    }

    @Test
    void anOlderChangeOfAnotherSpaceIsSentWithTheNewestCountsOfTheZone() {
        broadcaster.publish(change(1L, 100L, 3, 5));
        clock.advance(Duration.ofSeconds(2));

        broadcaster.publish(change(1L, 101L, 5, 4));

        OccupancyRealtimeMessage late = sent(2).get(1);
        assertThat(late.zone().freeCount()).isEqualTo(3);
        assertThat(late.spaces()).extracting(OccupancyRealtimeMessage.SpaceState::spaceId).containsExactly(101L);
    }

    private List<OccupancyRealtimeMessage> sent(int expected) {
        ArgumentCaptor<OccupancyRealtimeMessage> captor = ArgumentCaptor.forClass(OccupancyRealtimeMessage.class);
        verify(messaging, times(expected)).convertAndSend(eq(TOPIC), captor.capture());
        verify(messaging, times(expected)).convertAndSend(anyString(), any(Object.class));
        return captor.getAllValues();
    }

    private long nextVersion = 1;

    private ZoneOccupancyChange change(Long zoneId, Long spaceId, long free) {
        return change(zoneId, spaceId, free, nextVersion++);
    }

    private static ZoneOccupancyChange change(Long zoneId, Long spaceId, long free, long lotVersion) {
        return new ZoneOccupancyChange(7L, zoneId, new OccupancyCounts(10, free, 10 - free, 0),
                new OccupancyCounts(20, free, 20 - free, 0), false, T0,
                new SpaceChange(spaceId, SpaceStatus.OCCUPIED, null, T0), lotVersion);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

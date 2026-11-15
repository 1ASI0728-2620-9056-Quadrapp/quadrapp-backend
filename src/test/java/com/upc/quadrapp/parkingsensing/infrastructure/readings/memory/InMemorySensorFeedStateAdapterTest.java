package com.upc.quadrapp.parkingsensing.infrastructure.readings.memory;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingKind;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingOutcome;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each state has its own minimum detection time (occupied 10 s, freed 30 s here). Alternating
 * readings must keep the confirmed state and the pending transition.
 */
class InMemorySensorFeedStateAdapterTest {

    private static final long DEVICE = 7L;
    private static final int OCCUPIED_SECONDS = 10;
    private static final int FREED_SECONDS = 30;
    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private final InMemorySensorFeedStateAdapter feeds = new InMemorySensorFeedStateAdapter();
    private int sequence;

    @Test
    void freeIsConfirmedAfterItsOwnMinimumTimeWhenThresholdsDiffer() {
        assertThat(read(SensorState.OCCUPIED, 0).outcome()).isEqualTo(ReadingOutcome.PENDING);
        assertThat(read(SensorState.OCCUPIED, 10).outcome()).isEqualTo(ReadingOutcome.CONFIRMED);

        assertThat(read(SensorState.FREE, 20).outcome()).isEqualTo(ReadingOutcome.PENDING);
        assertThat(read(SensorState.FREE, 40).outcome()).isEqualTo(ReadingOutcome.PENDING);
        assertThat(read(SensorState.FREE, 50).outcome()).isEqualTo(ReadingOutcome.CONFIRMED);
    }

    @Test
    void aRepeatedConfirmedStateIsNotConfirmedAgain() {
        read(SensorState.OCCUPIED, 0);
        read(SensorState.OCCUPIED, 10);

        assertThat(read(SensorState.FREE, 15).outcome()).isEqualTo(ReadingOutcome.PENDING);
        assertThat(read(SensorState.OCCUPIED, 16).outcome()).isEqualTo(ReadingOutcome.BRIEF_CHANGE_DISCARDED);
        assertThat(read(SensorState.OCCUPIED, 30).outcome()).isEqualTo(ReadingOutcome.UNCHANGED);
    }

    private com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult read(
            SensorState state, int secondsAfterStart) {
        int minimum = state == SensorState.OCCUPIED ? OCCUPIED_SECONDS : FREED_SECONDS;
        SensorReading reading = new SensorReading("evt-" + (++sequence), "HW-1", ReadingKind.SPACE, state, null,
                T0.plusSeconds(secondsAfterStart), null, null, null);
        return feeds.accept(DEVICE, minimum, reading);
    }
}

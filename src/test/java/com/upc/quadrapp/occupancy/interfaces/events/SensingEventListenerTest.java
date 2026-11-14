package com.upc.quadrapp.occupancy.interfaces.events;

import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingsensing.domain.model.events.SensorStateChanged;
import com.upc.quadrapp.parkingsensing.domain.model.events.VehiclePassageDetected;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

class SensingEventListenerTest {

    private static final Instant DETECTED_AT = Instant.parse("2026-01-01T10:05:00Z");

    private final OccupancyCommandService commands = mock(OccupancyCommandService.class);
    private final SensingEventListener listener = new SensingEventListener(commands);

    @Test
    void occupiedSpaceIsRecordedAsOccupied() {
        listener.on(new SensorStateChanged("evt-1", 40L, 10L, 20L, 30L, SensorStateChanged.State.OCCUPIED, DETECTED_AT));

        verify(commands).handle(new RecordSensorEventCommand(
                10L, 20L, 40L, 30L, null, "evt-1", OccupancyEventType.OCCUPIED, DETECTED_AT));
    }

    @Test
    void freeSpaceIsRecordedAsFreed() {
        listener.on(new SensorStateChanged("evt-2", 40L, 10L, 20L, 30L, SensorStateChanged.State.FREE, DETECTED_AT));

        verify(commands).handle(new RecordSensorEventCommand(
                10L, 20L, 40L, 30L, null, "evt-2", OccupancyEventType.FREED, DETECTED_AT));
    }

    @Test
    void inboundPassageIsRecordedAsEntry() {
        listener.on(new VehiclePassageDetected("evt-3", 41L, 10L, 20L, 50L, VehiclePassageDetected.Direction.IN, DETECTED_AT));

        verify(commands).handle(new RecordSensorEventCommand(
                10L, 20L, 41L, null, 50L, "evt-3", OccupancyEventType.ENTRY, DETECTED_AT));
    }

    @Test
    void outboundPassageIsRecordedAsExit() {
        listener.on(new VehiclePassageDetected("evt-4", 41L, 10L, 20L, 50L, VehiclePassageDetected.Direction.OUT, DETECTED_AT));

        verify(commands).handle(new RecordSensorEventCommand(
                10L, 20L, 41L, null, 50L, "evt-4", OccupancyEventType.EXIT, DETECTED_AT));
    }

    @Test
    void anEventWithoutStateIsRejectedInsteadOfFreeingTheSpace() {
        assertThatThrownBy(() -> listener.on(new SensorStateChanged("evt-5", 40L, 10L, 20L, 30L, null, DETECTED_AT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sensor state is required.");
        verifyNoInteractions(commands);
    }

    @Test
    void aPassageWithoutDirectionIsRejectedInsteadOfCountingAnExit() {
        assertThatThrownBy(() -> listener.on(new VehiclePassageDetected("evt-6", 41L, 10L, 20L, 50L, null, DETECTED_AT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage direction is required.");
        verifyNoInteractions(commands);
    }
}

package com.upc.quadrapp.occupancy.interfaces.events;

import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingsensing.domain.model.events.SensorStateChanged;
import com.upc.quadrapp.parkingsensing.domain.model.events.VehiclePassageDetected;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Anticorruption layer for the events of Parking Sensing.
 * <p>
 * Each event arrives after the Sensing transaction commits, in a new transaction. Until the
 * handler completes, the publication stays incomplete in the registry and is published again
 * when the application restarts; duplicates are dropped by {@code eventId}.
 */
@Component
public class SensingEventListener {

    private final OccupancyCommandService commands;

    public SensingEventListener(OccupancyCommandService commands) {
        this.commands = commands;
    }

    @ApplicationModuleListener
    public void on(SensorStateChanged event) {
        commands.handle(new RecordSensorEventCommand(
                event.tenantId(),
                event.parkingLotId(),
                event.deviceId(),
                event.spaceId(),
                null,
                event.eventId(),
                event.state() == SensorStateChanged.State.OCCUPIED
                        ? OccupancyEventType.OCCUPIED
                        : OccupancyEventType.FREED,
                event.detectedAt()
        ));
    }

    @ApplicationModuleListener
    public void on(VehiclePassageDetected event) {
        commands.handle(new RecordSensorEventCommand(
                event.tenantId(),
                event.parkingLotId(),
                event.deviceId(),
                null,
                event.accessPointId(),
                event.eventId(),
                event.direction() == VehiclePassageDetected.Direction.IN
                        ? OccupancyEventType.ENTRY
                        : OccupancyEventType.EXIT,
                event.detectedAt()
        ));
    }
}

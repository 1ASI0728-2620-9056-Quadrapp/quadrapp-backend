package com.upc.quadrapp.occupancy.application.internal.commandservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancyReadPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancySnapshotPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SensorEventPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateParkingLotSnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.entities.SensorEvent;
import com.upc.quadrapp.occupancy.domain.model.exceptions.OccupancyNotFoundException;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyScopeType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingSensingConfigurationFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Clock;

@Service
public class OccupancyCommandServiceImpl implements OccupancyCommandService {

    private final SensorEventPersistencePort sensorEvents;
    private final OccupancyReadPersistencePort occupancyRead;
    private final OccupancySnapshotPersistencePort snapshots;
    private final ParkingSensingConfigurationFacade parkingConfiguration;
    private final ParkingConfigurationContextFacade parkingConfigurationContext;
    private final IamContextFacade iam;
    private final Clock clock;

    public OccupancyCommandServiceImpl(
            SensorEventPersistencePort sensorEvents,
            OccupancyReadPersistencePort occupancyRead,
            OccupancySnapshotPersistencePort snapshots,
            ParkingSensingConfigurationFacade parkingConfiguration,
            ParkingConfigurationContextFacade parkingConfigurationContext,
            IamContextFacade iam,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.sensorEvents = sensorEvents;
        this.occupancyRead = occupancyRead;
        this.snapshots = snapshots;
        this.parkingConfiguration = parkingConfiguration;
        this.parkingConfigurationContext = parkingConfigurationContext;
        this.iam = iam;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean handle(RecordSensorEventCommand command) {
        OccupancyEventType eventType = command.eventType();
        boolean spaceEvent = eventType == OccupancyEventType.OCCUPIED
                || eventType == OccupancyEventType.FREED;

        SensorEvent event = new SensorEvent(
                command.eventId(),
                command.deviceId(),
                command.spaceId(),
                command.accessId(),
                eventType,
                command.occurredAt(),
                clock.instant()
        );

        if (!sensorEvents.insertIfAbsent(event)) {
            return false;
        }

        if (spaceEvent) {
            parkingConfiguration.applyOccupancyDetection(
                    command.institutionId(),
                    command.parkingLotId(),
                    command.spaceId(),
                    eventType == OccupancyEventType.OCCUPIED,
                    command.occurredAt(),
                    event.receivedAt()
            );
        } else {
            parkingConfiguration.requireAccess(
                    command.institutionId(),
                    command.parkingLotId(),
                    command.accessId()
            );
        }

        return true;
    }

    @Override
    @Transactional
    public OccupancySnapshot handle(CreateOccupancySnapshotCommand command) {
        Long parkingLotId = occupancyRead.findParkingLotIdForZone(command.zoneId())
                .orElseThrow(
                        () -> new OccupancyNotFoundException("Zone not found.")
                );

        Long institutionId = iam.getCurrentUser().institutionId();
        if (parkingConfigurationContext.findParkingLot(institutionId, parkingLotId).isEmpty()) {
            throw new OccupancyNotFoundException("Zone not found.");
        }

        var counts = occupancyRead.summarize(
                OccupancyScopeType.ZONE,
                command.zoneId()
        );

        OccupancySnapshot snapshot = OccupancySnapshot.create(
                command.zoneId(),
                clock.instant(),
                counts
        );

        return snapshots.save(snapshot);
    }

    @Override
    @Transactional
    public java.util.List<OccupancySnapshot> handle(
            CreateParkingLotSnapshotCommand command
    ) {
        Long institutionId = iam.getCurrentUser().institutionId();
        if (parkingConfigurationContext
                .findParkingLot(institutionId, command.parkingLotId())
                .isEmpty()) {
            throw new OccupancyNotFoundException("Parking lot not found.");
        }

        java.time.Instant takenAt = clock.instant();
        return occupancyRead.findZoneIdsForParkingLot(command.parkingLotId())
                .stream()
                .map(zoneId -> OccupancySnapshot.create(
                        zoneId,
                        takenAt,
                        occupancyRead.summarize(OccupancyScopeType.ZONE, zoneId)
                ))
                .map(snapshots::save)
                .toList();
    }
}

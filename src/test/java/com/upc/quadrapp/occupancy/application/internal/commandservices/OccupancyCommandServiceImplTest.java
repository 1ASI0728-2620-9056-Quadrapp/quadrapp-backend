package com.upc.quadrapp.occupancy.application.internal.commandservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancyReadPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancySnapshotPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SensorEventPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingSensingConfigurationFacade;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OccupancyCommandServiceImplTest {

    private final SensorEventPersistencePort sensorEvents = mock(SensorEventPersistencePort.class);
    private final OccupancyReadPersistencePort occupancyRead = mock(OccupancyReadPersistencePort.class);
    private final OccupancySnapshotPersistencePort snapshots = mock(OccupancySnapshotPersistencePort.class);
    private final ParkingSensingConfigurationFacade parkingConfiguration =
            mock(ParkingSensingConfigurationFacade.class);
    private final ParkingConfigurationContextFacade parkingConfigurationContext =
            mock(ParkingConfigurationContextFacade.class);
    private final IamContextFacade iam = mock(IamContextFacade.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-01-01T10:05:01Z"),
            ZoneOffset.UTC
    );

    private final OccupancyCommandServiceImpl service = new OccupancyCommandServiceImpl(
            sensorEvents,
            occupancyRead,
            snapshots,
            parkingConfiguration,
            parkingConfigurationContext,
            iam,
            clock
    );

    @Test
    void duplicateEventDoesNotUpdateSpace() {
        when(sensorEvents.insertIfAbsent(any())).thenReturn(false);

        boolean recorded = service.handle(spaceCommand());

        assertFalse(recorded);
        verifyNoInteractions(parkingConfiguration);
    }

    @Test
    void newEventPersistsAndUpdatesSpaceInTheSameUseCase() {
        when(sensorEvents.insertIfAbsent(any())).thenReturn(true);

        boolean recorded = service.handle(spaceCommand());

        assertTrue(recorded);
        verify(parkingConfiguration).applyOccupancyDetection(
                eq(10L),
                eq(20L),
                eq(30L),
                eq(SpaceStatus.OCCUPIED),
                eq(Instant.parse("2026-01-01T10:05:00Z")),
                eq(Instant.parse("2026-01-01T10:05:01Z"))
        );
    }

    private static RecordSensorEventCommand spaceCommand() {
        return new RecordSensorEventCommand(
                10L,
                20L,
                40L,
                30L,
                null,
                "event-001",
                OccupancyEventType.OCCUPIED,
                Instant.parse("2026-01-01T10:05:00Z")
        );
    }
}

package com.upc.quadrapp.parkingsensing.infrastructure.events;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events.OccupancyDetectionPublisherPort;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OccupancyDetectionPublisherAdapter implements OccupancyDetectionPublisherPort {

    private final OccupancyContextFacade occupancy;

    public OccupancyDetectionPublisherAdapter(OccupancyContextFacade occupancy) {
        this.occupancy = occupancy;
    }

    @Override
    public void publishSpaceDetection(
            Long institutionId,
            Long parkingLotId,
            Long spaceId,
            Long deviceId,
            String eventId,
            SensorState state,
            Instant readingAt
    ) {
        occupancy.recordSpaceDetection(
                institutionId,
                parkingLotId,
                spaceId,
                deviceId,
                eventId,
                state == SensorState.OCCUPIED
                        ? OccupancyEventType.OCCUPIED
                        : OccupancyEventType.FREED,
                readingAt
        );
    }

    @Override
    public void publishPassageDetection(
            Long institutionId,
            Long parkingLotId,
            Long accessId,
            Long deviceId,
            String eventId,
            PassDirection direction,
            Instant readingAt
    ) {
        occupancy.recordPassageDetection(
                institutionId,
                parkingLotId,
                accessId,
                deviceId,
                eventId,
                direction == PassDirection.IN
                        ? OccupancyEventType.ENTRY
                        : OccupancyEventType.EXIT,
                readingAt
        );
    }
}

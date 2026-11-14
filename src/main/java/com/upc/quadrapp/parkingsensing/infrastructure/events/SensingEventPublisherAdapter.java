package com.upc.quadrapp.parkingsensing.infrastructure.events;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events.SensingEventPublisherPort;
import com.upc.quadrapp.parkingsensing.domain.model.events.SensorStateChanged;
import com.upc.quadrapp.parkingsensing.domain.model.events.VehiclePassageDetected;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes Spring application events. The Spring Modulith event publication registry stores
 * them in the current transaction and delivers them to the listeners after the commit.
 */
@Component
public class SensingEventPublisherAdapter implements SensingEventPublisherPort {

    private final ApplicationEventPublisher events;

    public SensingEventPublisherAdapter(ApplicationEventPublisher events) {
        this.events = events;
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
        events.publishEvent(new SensorStateChanged(
                eventId,
                deviceId,
                institutionId,
                parkingLotId,
                spaceId,
                state == SensorState.OCCUPIED
                        ? SensorStateChanged.State.OCCUPIED
                        : SensorStateChanged.State.FREE,
                readingAt
        ));
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
        events.publishEvent(new VehiclePassageDetected(
                eventId,
                deviceId,
                institutionId,
                parkingLotId,
                accessId,
                direction == PassDirection.IN
                        ? VehiclePassageDetected.Direction.IN
                        : VehiclePassageDetected.Direction.OUT,
                readingAt
        ));
    }
}

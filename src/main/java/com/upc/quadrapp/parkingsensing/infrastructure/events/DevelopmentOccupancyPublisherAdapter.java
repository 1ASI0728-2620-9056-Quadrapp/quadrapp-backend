package com.upc.quadrapp.parkingsensing.infrastructure.events;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events.OccupancyDetectionPublisherPort;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@Profile("dev")
public class DevelopmentOccupancyPublisherAdapter implements OccupancyDetectionPublisherPort {
    private static final Logger log = LoggerFactory.getLogger(DevelopmentOccupancyPublisherAdapter.class);
    private final ApplicationEventPublisher publisher;

    public DevelopmentOccupancyPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishSpaceDetection(Long institutionId, Long parkingLotId, Long spaceId,
                                      Long deviceId, String eventId, SensorState state, Instant readingAt) {
        log.warn("DEV ONLY: publishing non-durable SpaceDetectionEvent eventId={}", eventId);
        publisher.publishEvent(new SpaceDetectionEvent(institutionId, parkingLotId, spaceId,
                deviceId, eventId, state, readingAt));
    }

    @Override
    public void publishPassageDetection(Long institutionId, Long parkingLotId, Long accessId,
                                        Long deviceId, String eventId, PassDirection direction, Instant readingAt) {
        log.warn("DEV ONLY: publishing non-durable PassageDetectionEvent eventId={}", eventId);
        publisher.publishEvent(new PassageDetectionEvent(institutionId, parkingLotId, accessId,
                deviceId, eventId, direction, readingAt));
    }
}
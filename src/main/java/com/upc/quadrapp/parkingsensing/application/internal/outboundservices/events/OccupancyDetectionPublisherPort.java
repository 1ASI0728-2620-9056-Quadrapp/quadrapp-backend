package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import java.time.Instant;

public interface OccupancyDetectionPublisherPort {
    void publishSpaceDetection(Long institutionId, Long parkingLotId, Long spaceId,
                               Long deviceId, String eventId, SensorState state, Instant readingAt);
    void publishPassageDetection(Long institutionId, Long parkingLotId, Long accessId,
                                 Long deviceId, String eventId, PassDirection direction, Instant readingAt);
}
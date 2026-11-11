package com.upc.quadrapp.parkingsensing.infrastructure.events;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import java.time.Instant;

public record PassageDetectionEvent(Long institutionId, Long parkingLotId,
                                    Long accessId, Long deviceId, String eventId,
                                    PassDirection direction, Instant readingAt) {}
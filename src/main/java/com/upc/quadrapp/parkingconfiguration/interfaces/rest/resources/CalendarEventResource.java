package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.CalendarEventType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventImpact;

import java.time.Instant;

public record CalendarEventResource(Long id, Long institutionId, Long parkingLotId, Long zoneId,
        Long createdBy, String name, Instant startAt, Instant endAt, CalendarEventType type,
        EventImpact impact, Instant cancelledAt, Instant createdAt, Instant updatedAt) {}

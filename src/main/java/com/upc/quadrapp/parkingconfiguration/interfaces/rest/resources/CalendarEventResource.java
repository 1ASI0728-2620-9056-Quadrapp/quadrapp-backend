package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import java.time.Instant;

public record CalendarEventResource(Long id, Long institutionId, Long parkingLotId, Long zoneId,
        Long createdBy, String name, Instant startAt, Instant endAt) {}

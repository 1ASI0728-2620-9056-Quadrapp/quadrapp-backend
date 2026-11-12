package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import java.time.Instant;

public record ZoneResource(Long id, Long parkingLotId, String name, String description,
        int sortOrder, Instant createdAt, Instant updatedAt) {}

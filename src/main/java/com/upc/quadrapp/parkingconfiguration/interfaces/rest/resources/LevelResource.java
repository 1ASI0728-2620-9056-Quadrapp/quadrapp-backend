package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import java.time.Instant;

public record LevelResource(Long id, Long zoneId, String name, int sortOrder,
        Instant createdAt, Instant updatedAt) {}

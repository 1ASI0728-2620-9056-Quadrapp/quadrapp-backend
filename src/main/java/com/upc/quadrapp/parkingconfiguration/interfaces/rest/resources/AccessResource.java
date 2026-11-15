
package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.AccessType;

import java.time.Instant;

public record AccessResource(Long id, Long parkingLotId, String name, AccessType type, boolean flowCountingEnabled,
        Instant createdAt, Instant updatedAt) {}

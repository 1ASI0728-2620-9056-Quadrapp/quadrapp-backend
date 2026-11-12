package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.*;

import java.math.BigDecimal;
import java.time.Instant;

public record SpaceResource(Long id, Long levelId, String label, SpaceType type, BigDecimal posX, BigDecimal posY,
        SpaceStatus status, UnknownReason unknownReason, Instant statusSince, Instant lastEventAt,
        boolean active, Instant createdAt, Instant updatedAt) {}

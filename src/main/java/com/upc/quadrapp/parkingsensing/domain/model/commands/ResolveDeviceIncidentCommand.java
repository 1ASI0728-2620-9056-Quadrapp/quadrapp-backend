package com.upc.quadrapp.parkingsensing.domain.model.commands;

import java.time.Instant;
import java.util.Objects;

public record ResolveDeviceIncidentCommand(Long incidentId, Long actorUserId, Instant resolvedAt) {
    public ResolveDeviceIncidentCommand {
        if (incidentId == null || incidentId <= 0) throw new IllegalArgumentException("incidentId must be positive.");
        if (actorUserId == null || actorUserId <= 0) throw new IllegalArgumentException("actorUserId must be positive.");
        Objects.requireNonNull(resolvedAt, "resolvedAt is required.");
    }
}

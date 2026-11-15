package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Partial update of a device incident. The only supported change is resolving it, so
 * {@code status} must be {@code RESOLVED}.
 */
public record UpdateDeviceIncidentResource(
        @NotNull @Pattern(regexp = "RESOLVED", message = "must be RESOLVED") String status) {}

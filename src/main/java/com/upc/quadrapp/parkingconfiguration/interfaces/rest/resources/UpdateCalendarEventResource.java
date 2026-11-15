package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Partial update of a calendar event. The only supported change is cancelling it, so
 * {@code status} must be {@code CANCELLED}.
 */
public record UpdateCalendarEventResource(
        @NotNull @Pattern(regexp = "CANCELLED", message = "must be CANCELLED") String status) {}

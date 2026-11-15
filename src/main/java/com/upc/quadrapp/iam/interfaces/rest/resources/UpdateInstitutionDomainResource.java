package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Partial update of an institution domain. The only supported change is verifying it, so
 * {@code status} must be {@code VERIFIED}.
 */
public record UpdateInstitutionDomainResource(
        @NotNull @Pattern(regexp = "VERIFIED", message = "must be VERIFIED") String status) {}

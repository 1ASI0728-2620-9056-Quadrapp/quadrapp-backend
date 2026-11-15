package com.upc.quadrapp.iam.interfaces.rest.resources;

import java.time.Instant;

public record InstitutionResource(Long id, String name, boolean active,
        Instant createdAt, Instant updatedAt) {}

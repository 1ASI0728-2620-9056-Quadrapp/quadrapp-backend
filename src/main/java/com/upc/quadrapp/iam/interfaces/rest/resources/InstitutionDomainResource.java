package com.upc.quadrapp.iam.interfaces.rest.resources;

import java.time.Instant;

public record InstitutionDomainResource(Long id, Long institutionId, String domain, boolean verified,
        Long verifiedBy, Instant verifiedAt, Instant createdAt) {}

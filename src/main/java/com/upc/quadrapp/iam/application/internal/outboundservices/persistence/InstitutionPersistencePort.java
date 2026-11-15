package com.upc.quadrapp.iam.application.internal.outboundservices.persistence;

import com.upc.quadrapp.iam.domain.model.aggregates.Institution;

import java.util.Optional;

public interface InstitutionPersistencePort {
    Institution save(Institution institution);
    Optional<Institution> findById(Long institutionId);
    boolean existsByName(String name);
}


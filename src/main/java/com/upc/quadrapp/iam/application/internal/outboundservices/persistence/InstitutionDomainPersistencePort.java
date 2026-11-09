package com.upc.quadrapp.iam.application.internal.outboundservices.persistence;

import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;

import java.util.List;
import java.util.Optional;

public interface InstitutionDomainPersistencePort {
    InstitutionDomain save(InstitutionDomain institutionDomain);
    Optional<InstitutionDomain> findById(Long domainId);
    Optional<InstitutionDomain> findByDomain(String domain);
    List<InstitutionDomain> findByInstitutionId(Long institutionId);
}


package com.upc.quadrapp.iam.application.internal.outboundservices.persistence;

import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;

import java.util.List;
import java.util.Optional;

public interface InstitutionDomainPersistencePort {
    InstitutionDomain save(InstitutionDomain institutionDomain);
    Optional<InstitutionDomain> findById(Long domainId);
    /** The verified domain, if any: a domain can be claimed by several institutions but verified for one. */
    Optional<InstitutionDomain> findVerifiedByDomain(String domain);

    boolean existsByDomain(String domain);

    boolean existsByInstitutionIdAndDomain(Long institutionId, String domain);
    List<InstitutionDomain> findByInstitutionId(Long institutionId);
}


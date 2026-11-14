package com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.InstitutionDomainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstitutionDomainJpaRepository extends JpaRepository<InstitutionDomainJpaEntity, Long> {
    Optional<InstitutionDomainJpaEntity> findByDomainAndVerifiedAtIsNotNull(String domain);

    boolean existsByDomain(String domain);

    boolean existsByInstitutionIdAndDomain(Long institutionId, String domain);
    List<InstitutionDomainJpaEntity> findByInstitutionId(Long institutionId);
}

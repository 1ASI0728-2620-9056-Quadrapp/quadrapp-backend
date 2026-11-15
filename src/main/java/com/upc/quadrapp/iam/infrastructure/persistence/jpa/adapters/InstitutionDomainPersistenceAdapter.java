package com.upc.quadrapp.iam.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionDomainPersistencePort;
import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.mappers.IamJpaMapper;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories.InstitutionDomainJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InstitutionDomainPersistenceAdapter implements InstitutionDomainPersistencePort {
    private final InstitutionDomainJpaRepository repository;
    private final IamJpaMapper mapper;

    @Override
    public InstitutionDomain save(InstitutionDomain domain) {
        return mapper.toDomain(repository.save(mapper.toJpa(domain)));
    }

    @Override
    public Optional<InstitutionDomain> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<InstitutionDomain> findVerifiedByDomain(String domain) {
        return repository.findByDomainAndVerifiedAtIsNotNull(domain).map(mapper::toDomain);
    }

    @Override
    public boolean existsByDomain(String domain) {
        return repository.existsByDomain(domain);
    }

    @Override
    public boolean existsByInstitutionIdAndDomain(Long institutionId, String domain) {
        return repository.existsByInstitutionIdAndDomain(institutionId, domain);
    }

    @Override
    public List<InstitutionDomain> findByInstitutionId(Long institutionId
    ) {
        return repository.findByInstitutionId(institutionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

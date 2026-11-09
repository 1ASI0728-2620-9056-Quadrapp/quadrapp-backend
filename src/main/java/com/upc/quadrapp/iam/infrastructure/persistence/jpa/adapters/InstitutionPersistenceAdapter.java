package com.upc.quadrapp.iam.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionPersistencePort;
import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.mappers.IamJpaMapper;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories.InstitutionJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InstitutionPersistenceAdapter implements InstitutionPersistencePort {
    private final InstitutionJpaRepository repository;
    private final IamJpaMapper mapper;

    @Override
    public Institution save(Institution institution) {
        return mapper.toDomain(repository.save(mapper.toJpa(institution)));
    }

    @Override
    public Optional<Institution> findById(Long id) {return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {return repository.existsByName(name);
    }
}

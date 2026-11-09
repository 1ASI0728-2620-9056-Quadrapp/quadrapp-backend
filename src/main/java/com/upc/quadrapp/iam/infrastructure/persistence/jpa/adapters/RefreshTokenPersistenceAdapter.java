package com.upc.quadrapp.iam.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.RefreshTokenPersistencePort;
import com.upc.quadrapp.iam.domain.model.entities.RefreshToken;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.mappers.IamJpaMapper;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories.RefreshTokenJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenPersistencePort {
    private final RefreshTokenJpaRepository repository;
    private final IamJpaMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(repository.save(mapper.toJpa(token)));
    }

    @Override
    public Optional<RefreshToken> findForUpdateByTokenHash(String hash
    ) {
        return repository.findLockedByTokenHash(hash).map(mapper::toDomain);
    }
}

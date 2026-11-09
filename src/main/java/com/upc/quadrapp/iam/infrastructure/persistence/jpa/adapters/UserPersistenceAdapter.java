package com.upc.quadrapp.iam.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.UserPersistencePort;
import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.mappers.IamJpaMapper;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {
    private final UserJpaRepository repository;
    private final IamJpaMapper mapper;

    @Override
    public User save(User user) {
        return mapper.toDomain(repository.save(mapper.toJpa(user)));
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByInvitationTokenHash(String hash) {
        return repository.findByInvitationTokenHash(hash).map(mapper::toDomain);
    }

    @Override
    public List<User> findByInstitutionId(Long institutionId
    ) {
        return repository.findByInstitutionId(institutionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}

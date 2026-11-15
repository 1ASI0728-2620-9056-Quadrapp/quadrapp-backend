package com.upc.quadrapp.iam.application.internal.outboundservices.persistence;

import com.upc.quadrapp.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(Long userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByInvitationTokenHash(String tokenHash);
    List<User> findByInstitutionId(Long institutionId);
    boolean existsByEmail(String email);
}


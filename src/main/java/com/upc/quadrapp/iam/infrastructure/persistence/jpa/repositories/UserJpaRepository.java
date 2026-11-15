package com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByInvitationTokenHash(String tokenHash);
    List<UserJpaEntity> findByInstitutionId(Long institutionId);
    boolean existsByEmail(String email);
}

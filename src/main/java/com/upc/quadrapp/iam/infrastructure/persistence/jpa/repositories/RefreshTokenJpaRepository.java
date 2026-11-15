package com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.RefreshTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshTokenJpaEntity token
            WHERE token.tokenHash = :hash
            """)
    Optional<RefreshTokenJpaEntity> findLockedByTokenHash(@Param("hash") String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshTokenJpaEntity token
            SET token.revokedAt = :now
            WHERE token.familyId = :familyId AND token.revokedAt IS NULL
            """)
    int revokeActiveByFamilyId(@Param("familyId") UUID familyId, @Param("now") Instant now);
}

package com.upc.quadrapp.iam.application.internal.outboundservices.persistence;

import com.upc.quadrapp.iam.domain.model.entities.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenPersistencePort {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findForUpdateByTokenHash(String tokenHash);

    /** Revokes every still active token of the family and returns how many were revoked. */
    int revokeFamily(UUID familyId, Instant now);
}


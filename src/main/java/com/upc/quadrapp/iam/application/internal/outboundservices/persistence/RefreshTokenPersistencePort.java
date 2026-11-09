package com.upc.quadrapp.iam.application.internal.outboundservices.persistence;

import com.upc.quadrapp.iam.domain.model.entities.RefreshToken;

import java.util.Optional;

public interface RefreshTokenPersistencePort {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findForUpdateByTokenHash(String tokenHash);
}


package com.upc.quadrapp.iam.domain.services;

import com.upc.quadrapp.iam.domain.model.aggregates.User;

import java.time.Instant;

public record AuthenticationResult(
        User user,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt
) {}


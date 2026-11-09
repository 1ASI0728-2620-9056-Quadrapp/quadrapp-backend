package com.upc.quadrapp.iam.interfaces.rest.resources;

import java.time.Instant;

public record AuthenticatedUserResource(UserResource user, String accessToken, String refreshToken,
        long expiresIn, Instant expiresAt) {}

package com.upc.quadrapp.iam.application.internal.outboundservices.tokens;

import com.upc.quadrapp.iam.domain.model.aggregates.User;

import java.time.Instant;

public interface AccessTokenService {
    IssuedAccessToken issue(User user, Instant issuedAt);
}


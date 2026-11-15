package com.upc.quadrapp.iam.application.internal.outboundservices.tokens;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt) {}


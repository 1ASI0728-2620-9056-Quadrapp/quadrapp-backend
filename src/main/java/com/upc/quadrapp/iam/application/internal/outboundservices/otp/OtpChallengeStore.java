package com.upc.quadrapp.iam.application.internal.outboundservices.otp;

import java.time.Instant;

public interface OtpChallengeStore {

    boolean issue(
            String email,
            String codeHash,
            Instant expiresAt,
            Instant now
    );

    boolean consume(
            String email,
            String codeHash,
            Instant now
    );
}


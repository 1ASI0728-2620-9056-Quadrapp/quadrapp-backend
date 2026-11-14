package com.upc.quadrapp.iam.application.internal.outboundservices.otp;

import java.time.Duration;
import java.time.Instant;

public interface OtpChallengeStore {

    /**
     * Counts one access code request for the email and tells whether it is still within
     * {@code maxRequests} in the last {@code window}. Refused requests are not counted.
     */
    boolean registerRequest(
            String email,
            int maxRequests,
            Duration window,
            Instant now
    );

    /**
     * Stores a new code unless the previous one is still in its cooldown. A challenge that ran
     * out of attempts keeps its cooldown, so it cannot be replaced right away.
     */
    boolean issue(
            String email,
            String codeHash,
            Instant expiresAt,
            Instant now
    );

    /**
     * Checks the code and counts the attempt. After the maximum number of failed attempts the
     * challenge is locked: even the right code is rejected until a new one is issued.
     */
    boolean consume(
            String email,
            String codeHash,
            Instant now
    );
}

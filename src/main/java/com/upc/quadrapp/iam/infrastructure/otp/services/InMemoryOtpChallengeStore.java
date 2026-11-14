package com.upc.quadrapp.iam.infrastructure.otp.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.otp.OtpChallengeStore;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("dev")
public class InMemoryOtpChallengeStore implements OtpChallengeStore {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long ISSUE_COOLDOWN_SECONDS = 60;

    private final ConcurrentHashMap<String, Challenge> challenges =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Instant>> requests =
            new ConcurrentHashMap<>();

    /**
     * A challenge whose {@code hash} is null ran out of attempts: it rejects every code and
     * keeps {@code canIssueAt}, so the exhaustion does not reset the cooldown.
     */
    private record Challenge(
            String hash,
            Instant expiresAt,
            Instant canIssueAt,
            int failures
    ) {
        boolean locked() {
            return hash == null;
        }
    }

    @Override
    public boolean registerRequest(
            String email,
            int maxRequests,
            Duration window,
            Instant now
    ) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(window);
        Objects.requireNonNull(now);

        if (maxRequests <= 0 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException(
                    "OTP request limit must be positive."
            );
        }

        AtomicBoolean accepted = new AtomicBoolean(false);
        Instant windowStart = now.minus(window);

        requests.compute(email, (key, previous) -> {
            List<Instant> recent = new ArrayList<>();
            if (previous != null) {
                previous.stream()
                        .filter(requestedAt -> requestedAt.isAfter(windowStart))
                        .forEach(recent::add);
            }
            if (recent.size() < maxRequests) {
                recent.add(now);
                accepted.set(true);
            }
            return recent.isEmpty() ? null : List.copyOf(recent);
        });

        return accepted.get();
    }

    @Override
    public boolean issue(
            String email,
            String codeHash,
            Instant expiresAt,
            Instant now
    ) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(codeHash);
        Objects.requireNonNull(expiresAt);
        Objects.requireNonNull(now);

        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "OTP expiry must be in the future."
            );
        }

        AtomicBoolean accepted = new AtomicBoolean(false);

        challenges.compute(email, (key, previous) -> {

            if (previous != null &&
                    now.isBefore(previous.canIssueAt())) {
                return previous;
            }

            accepted.set(true);

            return new Challenge(
                    codeHash,
                    expiresAt,
                    now.plusSeconds(ISSUE_COOLDOWN_SECONDS),
                    0
            );
        });

        return accepted.get();
    }

    @Override
    public boolean consume(
            String email,
            String codeHash,
            Instant now
    ) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(codeHash);
        Objects.requireNonNull(now);

        AtomicBoolean accepted = new AtomicBoolean(false);

        challenges.computeIfPresent(email, (key, previous) -> {

            if (previous.locked()) {
                return previous;
            }

            if (!now.isBefore(previous.expiresAt())) {
                return null;
            }

            boolean matches = MessageDigest.isEqual(
                    previous.hash().getBytes(StandardCharsets.US_ASCII),
                    codeHash.getBytes(StandardCharsets.US_ASCII)
            );

            if (matches) {
                accepted.set(true);
                return null;
            }

            int attempts = previous.failures() + 1;

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                return new Challenge(
                        null,
                        previous.expiresAt(),
                        previous.canIssueAt(),
                        attempts
                );
            }

            return new Challenge(
                    previous.hash(),
                    previous.expiresAt(),
                    previous.canIssueAt(),
                    attempts
            );
        });

        return accepted.get();
    }
}

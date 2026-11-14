package com.upc.quadrapp.iam.domain.model.entities;

import com.upc.quadrapp.iam.domain.model.exceptions.IamConflictException;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class RefreshToken {

    private final Long id;
    private final Long userId;
    private final UUID familyId;
    private final String tokenHash;
    private final Instant expiresAt;

    private Instant revokedAt;

    private final Instant createdAt;

    private RefreshToken(
            Long id,
            Long userId,
            UUID familyId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            Instant createdAt
    ) {
        validateOptionalId(id);
        requirePositiveId(userId);
        validateTokenHash(tokenHash);

        this.id = id;
        this.userId = userId;
        this.familyId = Objects.requireNonNull(
                familyId, "Token family is required."
        );
        this.tokenHash = tokenHash;
        this.expiresAt = Objects.requireNonNull(
                expiresAt, "Expiration date is required."
        );
        this.revokedAt = revokedAt;
        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation date is required."
        );

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Expiration must be after creation."
            );
        }
    }

    /**
     * Issues the first token of a new family (a sign-in) or the next token of an existing
     * family (a rotation).
     */
    public static RefreshToken issue(
            Long userId,
            UUID familyId,
            String tokenHash,
            Instant expiresAt,
            Instant now
    ) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Token expiration must be in the future."
            );
        }

        return new RefreshToken(
                null,
                userId,
                familyId,
                tokenHash,
                expiresAt,
                null,
                now
        );
    }

    public static RefreshToken restore(
            Long id,
            Long userId,
            UUID familyId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            Instant createdAt
    ) {
        requirePositiveId(id);

        return new RefreshToken(
                id,
                userId,
                familyId,
                tokenHash,
                expiresAt,
                revokedAt,
                createdAt
        );
    }

    public boolean isValidAt(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (revokedAt != null) {
            throw new IamConflictException(
                    "Refresh token has already been revoked."
            );
        }

        this.revokedAt = now;
    }

    private static void validateTokenHash(String tokenHash) {
        if (tokenHash == null ||
                tokenHash.isBlank() ||
                tokenHash.length() > 255) {
            throw new IllegalArgumentException(
                    "Invalid refresh token hash."
            );
        }
    }

    private static void validateOptionalId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    "Identifier must be positive."
            );
        }
    }

    private static void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Valid identifier is required."
            );
        }
    }
}


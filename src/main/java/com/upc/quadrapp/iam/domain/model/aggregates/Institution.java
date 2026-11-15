package com.upc.quadrapp.iam.domain.model.aggregates;

import com.upc.quadrapp.iam.domain.model.exceptions.IamConflictException;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public class Institution {

    private final Long id;
    private String name;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Institution(
            Long id,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateId(id);

        this.id = id;
        this.name = validateName(name);
        this.active = active;
        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation date is required."
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt, "Update date is required."
        );
    }

    public static Institution create(String name, Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        return new Institution(
                null,
                name,
                true,
                now,
                now
        );
    }

    public static Institution restore(
            Long id,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Institution(
                id,
                name,
                active,
                createdAt,
                updatedAt
        );
    }


    public void rename(String newName, Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        this.name = validateName(newName);
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (active) {
            throw new IamConflictException(
                    "Institution is already active."
            );
        }

        this.active = true;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (!active) {
            throw new IamConflictException(
                    "Institution is already inactive."
            );
        }

        this.active = false;
        this.updatedAt = now;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Institution name is required."
            );
        }

        String normalized = name.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Institution name cannot exceed 150 characters."
            );
        }

        return normalized;
    }

    private static void validateId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    "Institution ID must be positive."
            );
        }
    }
}

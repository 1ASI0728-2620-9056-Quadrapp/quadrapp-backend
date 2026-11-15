package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Space {

    private final Long id;
    private final Long levelId;
    private final String label;

    private final SpaceType type;
    private final BigDecimal posX;
    private final BigDecimal posY;

    private final SpaceStatus status;
    private final UnknownReason unknownReason;

    private final Instant statusSince;
    private final Instant lastEventAt;

    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Space(
            Long id,
            Long levelId,
            String label,
            SpaceType type,
            BigDecimal posX,
            BigDecimal posY,
            SpaceStatus status,
            UnknownReason unknownReason,
            Instant statusSince,
            Instant lastEventAt,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (levelId == null ||
                levelId <= 0 ||
                (id != null && id <= 0)) {
            throw new IllegalArgumentException(
                    "Invalid space ID."
            );
        }

        this.id = id;
        this.levelId = levelId;
        this.label = new ConfigurationName(label).value();

        if (this.label.length() > 20) {
            throw new IllegalArgumentException(
                    "Space label too long."
            );
        }

        this.type = Objects.requireNonNull(type);
        this.posX = posX;
        this.posY = posY;

        this.status = Objects.requireNonNull(status);
        this.unknownReason = unknownReason;

        if ((status == SpaceStatus.UNKNOWN) !=
                (unknownReason != null)) {
            throw new IllegalArgumentException(
                    "Unknown status requires a reason and other states exclude it."
            );
        }

        this.statusSince = Objects.requireNonNull(statusSince);
        this.lastEventAt = lastEventAt;

        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Space create(
            Long levelId,
            String label,
            SpaceType type,
            BigDecimal posX,
            BigDecimal posY
    ) {
        var now = Instant.now();

        return new Space(
                null,
                levelId,
                label,
                type == null ? SpaceType.STANDARD : type,
                posX,
                posY,
                SpaceStatus.UNKNOWN,
                UnknownReason.NO_SENSOR,
                now,
                null,
                true,
                now,
                now
        );
    }

    public static Space restore(
            Long id,
            Long levelId,
            String label,
            SpaceType type,
            BigDecimal posX,
            BigDecimal posY,
            SpaceStatus status,
            UnknownReason reason,
            Instant since,
            Instant last,
            boolean active,
            Instant created,
            Instant updated
    ) {
        return new Space(
                id, levelId, label, type, posX, posY,
                status, reason, since, last,
                active, created, updated
        );
    }

    public Space applyOccupancyDetection(
            SpaceStatus newStatus,
            Instant occurredAt,
            Instant receivedAt
    ) {
        Objects.requireNonNull(newStatus);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(receivedAt);

        if (newStatus == SpaceStatus.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Sensor detections cannot set a space to UNKNOWN."
            );
        }

        if (lastEventAt != null && !occurredAt.isAfter(lastEventAt)) {
            return this;
        }

        boolean statusChanged = status != newStatus;

        return new Space(
                id,
                levelId,
                label,
                type,
                posX,
                posY,
                newStatus,
                null,
                statusChanged ? occurredAt : statusSince,
                occurredAt,
                active,
                createdAt,
                receivedAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getLevelId() {
        return levelId;
    }

    public String getLabel() {
        return label;
    }

    public SpaceType getType() {
        return type;
    }

    public BigDecimal getPosX() {
        return posX;
    }

    public BigDecimal getPosY() {
        return posY;
    }

    public SpaceStatus getStatus() {
        return status;
    }

    public UnknownReason getUnknownReason() {
        return unknownReason;
    }

    public Instant getStatusSince() {
        return statusSince;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

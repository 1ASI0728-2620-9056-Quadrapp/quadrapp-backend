
package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

import java.time.Instant;

public final class Level {

    private final Long id;
    private final Long zoneId;
    private final String name;
    private final int sortOrder;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Level(
            Long id,
            Long zoneId,
            String name,
            int sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (zoneId == null ||
                zoneId <= 0 ||
                (id != null && id <= 0)) {
            throw new IllegalArgumentException(
                    "Invalid level ID."
            );
        }

        this.id = id;
        this.zoneId = zoneId;
        this.name = new ConfigurationName(name).value();

        if (this.name.length() > 40) {
            throw new IllegalArgumentException(
                    "Level name too long."
            );
        }

        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Level create(
            Long zoneId,
            String name,
            int sortOrder
    ) {
        var now = Instant.now();

        return new Level(
                null, zoneId, name, sortOrder, now, now
        );
    }

    public static Level restore(
            Long id,
            Long zoneId,
            String name,
            int sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Level(
                id, zoneId, name, sortOrder, createdAt, updatedAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

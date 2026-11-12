package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

import java.time.Instant;

public final class Zone {

    private final Long id;
    private final Long parkingLotId;

    private final String name;
    private final String description;
    private final int sortOrder;

    private final Instant createdAt;
    private final Instant updatedAt;

    private Zone(
            Long id,
            Long parkingLotId,
            String name,
            String description,
            int sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (parkingLotId == null ||
                parkingLotId <= 0 ||
                (id != null && id <= 0)) {
            throw new IllegalArgumentException(
                    "Invalid zone ID."
            );
        }

        this.id = id;
        this.parkingLotId = parkingLotId;
        this.name = new ConfigurationName(name).value();

        if (this.name.length() > 60) {
            throw new IllegalArgumentException(
                    "Zone name too long."
            );
        }

        if (description != null &&
                description.length() > 80) {
            throw new IllegalArgumentException(
                    "Zone description too long."
            );
        }

        this.description = description;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Zone create(
            Long parkingLotId,
            String name,
            String description,
            int sortOrder
    ) {
        var now = Instant.now();

        return new Zone(
                null, parkingLotId, name, description,
                sortOrder, now, now
        );
    }

    public static Zone restore(
            Long id,
            Long parkingLotId,
            String name,
            String description,
            int sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Zone(
                id, parkingLotId, name, description,
                sortOrder, createdAt, updatedAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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

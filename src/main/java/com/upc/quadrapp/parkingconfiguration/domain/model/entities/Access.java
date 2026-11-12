package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.AccessType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

import java.time.Instant;
import java.util.Objects;

public final class Access {

    private final Long id;
    private final Long parkingLotId;
    private final String name;
    private final AccessType type;
    private final boolean flowCountingEnabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Access(
            Long id,
            Long parkingLotId,
            String name,
            AccessType type,
            boolean flowCountingEnabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (parkingLotId == null ||
                parkingLotId <= 0 ||
                (id != null && id <= 0)) {
            throw new IllegalArgumentException(
                    "Invalid access ID."
            );
        }

        this.id = id;
        this.parkingLotId = parkingLotId;
        this.name = new ConfigurationName(name).value();

        if (this.name.length() > 60) {
            throw new IllegalArgumentException(
                    "Access name too long."
            );
        }

        this.type = Objects.requireNonNull(
                type, "Access type is required."
        );
        this.flowCountingEnabled = flowCountingEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Access create(
            Long parkingLotId,
            String name,
            AccessType type,
            boolean flowCountingEnabled
    ) {
        var now = Instant.now();

        return new Access(
                null, parkingLotId, name, type,
                flowCountingEnabled, now, now
        );
    }

    public static Access restore(
            Long id,
            Long parkingLotId,
            String name,
            AccessType type,
            boolean flowCountingEnabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Access(
                id, parkingLotId, name, type,
                flowCountingEnabled, createdAt, updatedAt
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

    public AccessType getType() {
        return type;
    }

    public boolean isFlowCountingEnabled() {
        return flowCountingEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

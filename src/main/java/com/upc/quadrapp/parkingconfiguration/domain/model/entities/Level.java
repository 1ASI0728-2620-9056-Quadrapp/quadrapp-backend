package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

public final class Level {

    private final Long id;
    private final Long zoneId;
    private final String name;

    private Level(
            Long id,
            Long zoneId,
            String name
    ) {
        if (id != null) {
            requirePositive(id, "id");
        }

        requirePositive(zoneId, "zoneId");

        this.id = id;
        this.zoneId = zoneId;
        this.name = new ConfigurationName(name).value();
    }

    public static Level create(
            Long zoneId,
            String name
    ) {
        return new Level(
                null,
                zoneId,
                name
        );
    }

    public static Level restore(
            Long id,
            Long zoneId,
            String name
    ) {
        requirePositive(id, "id");

        return new Level(
                id,
                zoneId,
                name
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

    private static void requirePositive(
            Long value,
            String field
    ) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    field + " must be positive."
            );
        }
    }
}

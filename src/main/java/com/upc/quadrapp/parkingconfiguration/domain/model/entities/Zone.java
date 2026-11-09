package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

public final class Zone {

    private final Long id;
    private final Long parkingLotId;
    private final String name;

    private Zone(
            Long id,
            Long parkingLotId,
            String name
    ) {
        if (id != null) {
            requirePositive(id, "id");
        }

        requirePositive(parkingLotId, "parkingLotId");

        this.id = id;
        this.parkingLotId = parkingLotId;
        this.name = new ConfigurationName(name).value();
    }

    public static Zone create(
            Long parkingLotId,
            String name
    ) {
        return new Zone(
                null,
                parkingLotId,
                name
        );
    }

    public static Zone restore(
            Long id,
            Long parkingLotId,
            String name
    ) {
        requirePositive(id, "id");

        return new Zone(
                id,
                parkingLotId,
                name
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

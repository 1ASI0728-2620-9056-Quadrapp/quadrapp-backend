package com.upc.quadrapp.parkingconfiguration.domain.model.aggregates;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

public final class ParkingLot {

    private final Long id;
    private final Long institutionId;
    private final String name;

    private ParkingLot(Long id, Long institutionId, String name
    ) {
        if (id != null) {
            requirePositive(id, "id");
        }

        requirePositive(institutionId, "institutionId");

        this.id = id;
        this.institutionId = institutionId;
        this.name = new ConfigurationName(name).value();
    }

    public static ParkingLot register(
            Long institutionId,
            String name
    ) {
        return new ParkingLot(
                null,
                institutionId,
                name
        );
    }

    public static ParkingLot restore(
            Long id,
            Long institutionId,
            String name
    ) {
        requirePositive(id, "id");

        return new ParkingLot(
                id,
                institutionId,
                name
        );
    }

    public Long getId() {
        return id;
    }

    public Long getInstitutionId() {
        return institutionId;
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

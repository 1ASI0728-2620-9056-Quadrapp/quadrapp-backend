
package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

public final class Space {

    private final Long id;
    private final Long levelId;
    private final String label;

    private Space(
            Long id,
            Long levelId,
            String label
    ) {
        if (id != null) {
            requirePositive(id, "id");
        }

        requirePositive(levelId, "levelId");

        this.id = id;
        this.levelId = levelId;
        this.label = new ConfigurationName(label).value();
    }

    public static Space create(
            Long levelId,
            String label
    ) {
        return new Space(
                null,
                levelId,
                label
        );
    }

    public static Space restore(
            Long id,
            Long levelId,
            String label
    ) {
        requirePositive(id, "id");

        return new Space(
                id,
                levelId,
                label
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

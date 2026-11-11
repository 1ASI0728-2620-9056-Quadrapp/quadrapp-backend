package com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects;

public record ConfigurationName(String value) {

    public ConfigurationName {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Name is required."
            );
        }

        value = value.trim().replaceAll("\\s+", " ");
    }
}

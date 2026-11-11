package com.upc.quadrapp.parkingsensing.domain.model.valueobjects;

public record HardwareIdentifier(String value) {
    public HardwareIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("hardwareId is required.");
        }
        value = value.trim();
        if (value.length() > 128) {
            throw new IllegalArgumentException("hardwareId is too long.");
        }
    }
}

package com.upc.quadrapp.iam.domain.model.valueobjects;

import java.util.Locale;

public record EmailAddress(String value) {

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > 254 ||
                !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(
                    "Invalid email address."
            );
        }
    }

    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }
}

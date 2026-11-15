package com.upc.quadrapp.iam.domain.model.valueobjects;

import java.util.Locale;

public record InstitutionDomainName(String value) {

    public InstitutionDomainName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Institution domain is required."
            );
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > 120 ||
                !value.matches(
                        "^[a-z0-9]+([.-][a-z0-9]+)*\\.[a-z]{2,}$"
                )) {
            throw new IllegalArgumentException(
                    "Invalid institution domain."
            );
        }
    }

    public boolean matches(EmailAddress email) {
        if (email == null) {
            return false;
        }

        return value.equals(email.domain());
    }
}

package com.upc.quadrapp.iam.domain.model.valueobjects;

public enum Roles {
    DRIVER,
    OPERATOR,
    ADMIN,
    PLATFORM_ADMIN;

    public boolean isInstitutional() {
        return this != PLATFORM_ADMIN;
    }

    public boolean canBeInvited() {
        return this == OPERATOR || this == ADMIN;
    }
}

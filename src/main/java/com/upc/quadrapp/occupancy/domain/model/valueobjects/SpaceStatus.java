package com.upc.quadrapp.occupancy.domain.model.valueobjects;

/** Business state of a space. An UNKNOWN space never counts as free. */
public enum SpaceStatus {
    FREE,
    OCCUPIED,
    UNKNOWN
}

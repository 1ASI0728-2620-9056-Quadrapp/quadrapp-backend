package com.upc.quadrapp.occupancy.domain.model.valueobjects;

/** Why a space is UNKNOWN; the console shows it to the operator. */
public enum UnknownReason {
    NOT_YET_REPORTED,
    NO_SENSOR,
    SENSOR_FAULT,
    GATEWAY_OFFLINE,
    AWAITING_STABLE_DETECTION
}

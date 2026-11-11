package com.upc.quadrapp.parkingsensing.domain.model.valueobjects;

public enum ReadingOutcome {
    PENDING,
    CONFIRMED,
    UNCHANGED,
    BRIEF_CHANGE_DISCARDED,
    OUT_OF_ORDER,
    PASSAGE_RECORDED,
    HEARTBEAT_ACCEPTED
}

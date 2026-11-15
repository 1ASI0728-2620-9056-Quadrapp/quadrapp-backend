package com.upc.quadrapp.prediction.domain.model.valueobjects;

/** What started a forecast generation run. */
public enum ForecastTrigger {
    /** The 5-minute scheduler (or a client asking for the run of the current slot). */
    SCHEDULED,
    /** A campus event that affects the next 60 minutes was registered or cancelled. */
    CAMPUS_EVENT
}

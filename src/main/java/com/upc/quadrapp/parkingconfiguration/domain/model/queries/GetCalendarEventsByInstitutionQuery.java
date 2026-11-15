package com.upc.quadrapp.parkingconfiguration.domain.model.queries;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

import java.time.Instant;

public record GetCalendarEventsByInstitutionQuery(Long institutionId, Instant from, Instant to) {
    public GetCalendarEventsByInstitutionQuery {

        if (institutionId == null || institutionId <= 0) {
            throw new IllegalArgumentException(
                    "institutionId must be positive."
            );
        }

        new EventWindow(from, to);
    }
}

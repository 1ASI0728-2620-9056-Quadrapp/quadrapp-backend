package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CalendarEventPersistencePort {

    CalendarEvent save(CalendarEvent value);

    Optional<CalendarEvent> findById(Long id);

    List<CalendarEvent> findOverlappingByInstitutionId(
            Long institutionId,
            Instant from,
            Instant to
    );
}

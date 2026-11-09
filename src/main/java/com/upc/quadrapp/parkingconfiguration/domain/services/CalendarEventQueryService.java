package com.upc.quadrapp.parkingconfiguration.domain.services;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetCalendarEventsByInstitutionQuery;

import java.util.List;

public interface CalendarEventQueryService {

    List<CalendarEvent> handle(
            GetCalendarEventsByInstitutionQuery query
    );
}

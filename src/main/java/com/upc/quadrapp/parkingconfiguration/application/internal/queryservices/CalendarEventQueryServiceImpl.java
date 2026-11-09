package com.upc.quadrapp.parkingconfiguration.application.internal.queryservices;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.CalendarEventPersistencePort;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetCalendarEventsByInstitutionQuery;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventQueryService;

import java.util.List;

public final class CalendarEventQueryServiceImpl
        implements CalendarEventQueryService {

    private final CalendarEventPersistencePort events;
    private final ParkingConfigurationAuthorizationPort authorization;

    public CalendarEventQueryServiceImpl(
            CalendarEventPersistencePort events,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        this.events = events;
        this.authorization = authorization;
    }

    @Override
    public List<CalendarEvent> handle(
            GetCalendarEventsByInstitutionQuery query
    ) {

        authorization.requireInstitutionRead(
                query.institutionId()
        );

        return events.findOverlappingByInstitutionId(
                query.institutionId(),
                query.from(),
                query.to()
        );
    }
}

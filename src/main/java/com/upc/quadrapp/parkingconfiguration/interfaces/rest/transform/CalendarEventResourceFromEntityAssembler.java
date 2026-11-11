package com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.CalendarEventResource;

public final class CalendarEventResourceFromEntityAssembler {

    private CalendarEventResourceFromEntityAssembler() {}

    public static CalendarEventResource toResourceFromEntity(
            CalendarEvent event
    ) {
        return new CalendarEventResource(
                event.getId(),
                event.getInstitutionId(),
                event.getParkingLotId(),
                event.getZoneId(),
                event.getCreatedBy(),
                event.getName(),
                event.getWindow().startAt(),
                event.getWindow().endAt()
        );
    }
}

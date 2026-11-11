package com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform;

import com.upc.quadrapp.parkingconfiguration.domain.model.commands.ScheduleCalendarEventCommand;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.ScheduleCalendarEventResource;

public final class CalendarEventCommandFromResourceAssembler {

    private CalendarEventCommandFromResourceAssembler() {}

    public static ScheduleCalendarEventCommand toCommand(
            Long institutionId,
            Long actorId,
            ScheduleCalendarEventResource resource
    ) {
        return new ScheduleCalendarEventCommand(
                institutionId,
                resource.parkingLotId(),
                resource.zoneId(),
                actorId,
                resource.name(),
                resource.startAt(),
                resource.endAt()
        );
    }
}

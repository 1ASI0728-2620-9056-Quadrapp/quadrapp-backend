package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.CalendarEventType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventImpact;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

import java.time.Instant;

public record ScheduleCalendarEventCommand(Long institutionId, Long parkingLotId, Long zoneId, Long createdBy,
        String name, Instant startAt, Instant endAt, CalendarEventType type, EventImpact impact)
{
    public ScheduleCalendarEventCommand {

        if (institutionId == null || institutionId <= 0 || name == null || name.isBlank() || type == null) {
            throw new IllegalArgumentException("Institution, name and event type are required.");
        }

        if (zoneId != null && parkingLotId == null) {
            throw new IllegalArgumentException("zoneId requires parkingLotId.");
        }

        new EventWindow(startAt, endAt);
    }
}

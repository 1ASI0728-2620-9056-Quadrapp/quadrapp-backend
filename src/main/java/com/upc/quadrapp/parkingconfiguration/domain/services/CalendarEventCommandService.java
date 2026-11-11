package com.upc.quadrapp.parkingconfiguration.domain.services;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.ScheduleCalendarEventCommand;

public interface CalendarEventCommandService {

    CalendarEvent handle(ScheduleCalendarEventCommand command);
}

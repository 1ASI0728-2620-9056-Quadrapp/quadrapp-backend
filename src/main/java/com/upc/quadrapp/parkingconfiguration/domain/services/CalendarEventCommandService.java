package com.upc.quadrapp.parkingconfiguration.domain.services;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CancelCalendarEventCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.ScheduleCalendarEventCommand;

public interface CalendarEventCommandService {

    CalendarEvent handle(ScheduleCalendarEventCommand command);

    /**
     * Cancels a scheduled event once. A missing event or one of another institution is not
     * found; an event that is already cancelled is a conflict.
     */
    CalendarEvent handle(CancelCalendarEventCommand command);
}

package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

public record CancelCalendarEventCommand(Long institutionId, Long calendarEventId) {

    public CancelCalendarEventCommand {
        if (institutionId == null || institutionId <= 0 || calendarEventId == null || calendarEventId <= 0) {
            throw new IllegalArgumentException("Institution and calendar event are required.");
        }
    }
}

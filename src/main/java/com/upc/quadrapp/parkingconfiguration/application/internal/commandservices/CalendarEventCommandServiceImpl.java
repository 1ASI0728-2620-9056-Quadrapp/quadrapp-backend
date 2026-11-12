package com.upc.quadrapp.parkingconfiguration.application.internal.commandservices;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.*;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.ScheduleCalendarEventCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventCommandService;

import org.springframework.security.access.AccessDeniedException;

public final class CalendarEventCommandServiceImpl implements CalendarEventCommandService {

    private final CalendarEventPersistencePort events;
    private final ParkingLotPersistencePort lots;
    private final ZonePersistencePort zones;
    private final ParkingConfigurationAuthorizationPort authorization;

    public CalendarEventCommandServiceImpl(
            CalendarEventPersistencePort events,
            ParkingLotPersistencePort lots,
            ZonePersistencePort zones,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        this.events = events;
        this.lots = lots;
        this.zones = zones;
        this.authorization = authorization;
    }

    @Override
    public CalendarEvent handle(
            ScheduleCalendarEventCommand command
    ) {
        authorization.requireInstitutionWrite(
                command.institutionId()
        );

        if (command.parkingLotId() != null) {

            var lot = lots.findById(command.parkingLotId())
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Parking lot not found."
                            )
                    );

            if (!lot.getInstitutionId().equals(
                    command.institutionId()
            )) {
                throw new AccessDeniedException(
                        "Parking lot belongs to another institution."
                );
            }
        }

        if (command.zoneId() != null) {

            var zone = zones.findById(command.zoneId())
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Zone not found."
                            )
                    );

            if (!zone.getParkingLotId().equals(
                    command.parkingLotId()
            )) {
                throw new IllegalArgumentException(
                        "Zone belongs to another parking lot."
                );
            }
        }

        CalendarEvent event = CalendarEvent.schedule(
                command.institutionId(),
                command.parkingLotId(),
                command.zoneId(),
                command.createdBy(),
                command.name(),
                new EventWindow(
                        command.startAt(),
                        command.endAt()
                ),
                command.type(),
                command.impact()
        );

        return events.save(event);
    }
}

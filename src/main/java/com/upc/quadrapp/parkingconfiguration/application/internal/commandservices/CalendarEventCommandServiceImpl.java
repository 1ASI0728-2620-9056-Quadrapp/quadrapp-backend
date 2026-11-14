package com.upc.quadrapp.parkingconfiguration.application.internal.commandservices;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.*;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CancelCalendarEventCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.ScheduleCalendarEventCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventCancelled;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventProvided;
import com.upc.quadrapp.parkingconfiguration.domain.model.exceptions.ParkingConfigurationNotFoundException;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;

/**
 * Schedules and cancels calendar events. Each change is saved and its event
 * ({@code CampusEventProvided} or {@code CampusEventCancelled}) is published in the same
 * transaction, so the event publication registry delivers it only if the change commits.
 */
public final class CalendarEventCommandServiceImpl implements CalendarEventCommandService {

    private final CalendarEventPersistencePort events;
    private final ParkingLotPersistencePort lots;
    private final ZonePersistencePort zones;
    private final ParkingConfigurationAuthorizationPort authorization;
    private final ApplicationEventPublisher publisher;
    private final TransactionOperations transactions;

    public CalendarEventCommandServiceImpl(
            CalendarEventPersistencePort events,
            ParkingLotPersistencePort lots,
            ZonePersistencePort zones,
            ParkingConfigurationAuthorizationPort authorization,
            ApplicationEventPublisher publisher,
            TransactionOperations transactions
    ) {
        this.events = events;
        this.lots = lots;
        this.zones = zones;
        this.authorization = authorization;
        this.publisher = publisher;
        this.transactions = transactions;
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

            // A lot of another institution reads as missing, so its existence is not revealed.
            if (!lot.getInstitutionId().equals(
                    command.institutionId()
            )) {
                throw new IllegalArgumentException(
                        "Parking lot not found."
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

        return transactions.execute(status -> {
            CalendarEvent saved = events.save(event);
            publisher.publishEvent(new CampusEventProvided(
                    saved.getId(),
                    saved.getInstitutionId(),
                    saved.getParkingLotId(),
                    saved.getWindow().startAt(),
                    saved.getWindow().endAt(),
                    saved.getImpact().name()
            ));
            return saved;
        });
    }

    @Override
    public CalendarEvent handle(CancelCalendarEventCommand command) {
        authorization.requireInstitutionWrite(command.institutionId());

        return transactions.execute(status -> {
            CalendarEvent event = events.findById(command.calendarEventId())
                    // An event of another institution reads as missing, so its existence is not revealed.
                    .filter(found -> found.getInstitutionId().equals(command.institutionId()))
                    .orElseThrow(() -> new ParkingConfigurationNotFoundException("Calendar event not found."));
            if (event.getCancelledAt() != null) {
                throw new IllegalStateException("Calendar event is already cancelled.");
            }

            Instant now = Instant.now();
            CalendarEvent cancelled = events.save(CalendarEvent.restore(
                    event.getId(),
                    event.getInstitutionId(),
                    event.getParkingLotId(),
                    event.getZoneId(),
                    event.getCreatedBy(),
                    event.getName(),
                    event.getWindow(),
                    event.getType(),
                    event.getImpact(),
                    now,
                    event.getCreatedAt(),
                    now
            ));
            publisher.publishEvent(new CampusEventCancelled(
                    cancelled.getId(),
                    cancelled.getInstitutionId(),
                    cancelled.getParkingLotId(),
                    cancelled.getWindow().startAt(),
                    cancelled.getWindow().endAt()
            ));
            return cancelled;
        });
    }
}

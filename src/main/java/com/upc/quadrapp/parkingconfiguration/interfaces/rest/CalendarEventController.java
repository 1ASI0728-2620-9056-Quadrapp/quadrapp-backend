package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CancelCalendarEventCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetCalendarEventsByInstitutionQuery;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventQueryService;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.CalendarEventCommandFromResourceAssembler;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.CalendarEventResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/calendar-events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Calendar Events", description = "Schedule, cancel, and query campus calendar events.")
public class CalendarEventController {

    private final CalendarEventCommandService commands;
    private final CalendarEventQueryService queries;
    private final IamContextFacade iam;

    @Operation(
            operationId = "scheduleCalendarEvent",
            summary = "Schedule a calendar event",
            description = "Institution administrators. Schedules a campus event for a parking lot, or one of "
                    + "its zones, that the forecasts take into account (201); 400 for invalid data or an "
                    + "unknown parking lot or zone and 403 for other roles."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CalendarEventResource> schedule(
            @Valid @RequestBody ScheduleCalendarEventResource resource
    ) {
        var actor = iam.getCurrentUser();

        Long institutionId = requireInstitutionId(
                actor.institutionId()
        );

        var created = commands.handle(
                CalendarEventCommandFromResourceAssembler.toCommand(
                        institutionId,
                        actor.userId(),
                        resource
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        CalendarEventResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    /**
     * Cancels a scheduled event (US35, scenario 3) with {@code {"status": "CANCELLED"}}: 200 with
     * the cancelled event, which is kept, 409 if it was already cancelled, 404 if it does not exist
     * or belongs to another institution, 400 for any other status.
     */
    @Operation(
            operationId = "cancelCalendarEvent",
            summary = "Cancel a calendar event",
            description = "Institution administrators. Send {\"status\": \"CANCELLED\"}; any other status is 400. "
                    + "The event is kept and its forecasts are regenerated (200); 403 for other roles, 404 "
                    + "when it does not exist or belongs to another institution and 409 when it is already "
                    + "cancelled."
    )
    @PatchMapping(
            value = "/{calendarEventId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CalendarEventResource> cancel(
            @PathVariable Long calendarEventId,
            @Valid @RequestBody UpdateCalendarEventResource resource
    ) {
        Long institutionId = requireInstitutionId(
                iam.getCurrentUser().institutionId()
        );

        var cancelled = commands.handle(
                new CancelCalendarEventCommand(institutionId, calendarEventId)
        );

        return ResponseEntity.ok(
                CalendarEventResourceFromEntityAssembler.toResourceFromEntity(cancelled)
        );
    }

    @Operation(
            operationId = "listCalendarEvents",
            summary = "List the calendar events of the institution",
            description = "Any user of an institution. Returns the events between the required from and to "
                    + "instants (ISO-8601) (200); 400 when a bound is missing or invalid and 403 for users "
                    + "without an institution."
    )
    @GetMapping
    public ResponseEntity<List<CalendarEventResource>> list(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        Long institutionId = requireInstitutionId(
                iam.getCurrentUser().institutionId()
        );

        var result = queries.handle(
                new GetCalendarEventsByInstitutionQuery(
                        institutionId,
                        from,
                        to
                )
        );

        return ResponseEntity.ok(
                result.stream()
                        .map(
                                CalendarEventResourceFromEntityAssembler
                                        ::toResourceFromEntity
                        )
                        .toList()
        );
    }

    private Long requireInstitutionId(Long institutionId) {

        if (institutionId == null) {
            throw new AccessDeniedException(
                    "An institution-scoped user is required."
            );
        }

        return institutionId;
    }
}

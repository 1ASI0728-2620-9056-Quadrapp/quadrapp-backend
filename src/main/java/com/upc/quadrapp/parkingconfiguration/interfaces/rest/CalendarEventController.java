package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetCalendarEventsByInstitutionQuery;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.CalendarEventQueryService;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.CalendarEventCommandFromResourceAssembler;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.CalendarEventResourceFromEntityAssembler;

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

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/calendar-events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Calendar Events", description = "")
public class CalendarEventController {

    private final CalendarEventCommandService commands;
    private final CalendarEventQueryService queries;
    private final IamContextFacade iam;

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

package com.upc.quadrapp.parkingsensing.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;

import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceIncidentsQuery;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentCommandService;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentQueryService;

import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.*;
import com.upc.quadrapp.parkingsensing.interfaces.rest.transform.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Tag(name = "Device Incidents", description = "Report, query, and resolve IoT device incidents.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/device-incidents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DeviceIncidentController {

    private final DeviceIncidentCommandService commands;
    private final DeviceIncidentQueryService queries;
    private final IamContextFacade iam;
    private final Clock clock;

    @Operation(
            operationId = "reportDeviceIncident",
            summary = "Report a device incident",
            description = "Institution administrators. Records a USER_REPORT incident of a device identified by "
                    + "deviceId or hardwareId (201); 400 for invalid data or a device of another "
                    + "institution or parking lot, 403 for other roles and 404 when the device does not "
                    + "exist."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceIncidentResource> report(
            @Valid @RequestBody ReportDeviceIncidentResource resource
    ) {
        var actor = iam.getCurrentUser();

        if (actor == null ||
                actor.institutionId() == null ||
                actor.userId() == null) {
            throw new AccessDeniedException(
                    "Authenticated institutional user required."
            );
        }

        var created = commands.handle(
                DeviceIncidentCommandFromResourceAssembler.toCommand(
                        actor.institutionId(),
                        actor.userId(),
                        resource,
                        Instant.now(clock)
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        DeviceIncidentResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    @Operation(
            operationId = "listDeviceIncidents",
            summary = "List device incidents",
            description = "Any user of an institution. Returns the incidents of the institution, newest first, "
                    + "optionally of one parking lot and with one status, OPEN or RESOLVED (200); 400 for "
                    + "any other status."
    )
    @GetMapping
    public ResponseEntity<List<DeviceIncidentResource>> list(
            @RequestParam(required = false)
            @Positive Long parkingLotId,

            @RequestParam(required = false)
            String status
    ) {
        var incidents = queries.handle(
                new GetDeviceIncidentsQuery(
                        currentInstitutionId(),
                        parkingLotId,
                        statusFilter(status)
                )
        );

        return ResponseEntity.ok(
                incidents.stream()
                        .map(
                                DeviceIncidentResourceFromEntityAssembler
                                        ::toResourceFromEntity
                        )
                        .toList()
        );
    }

    @Operation(
            operationId = "resolveDeviceIncident",
            summary = "Resolve a device incident",
            description = "Institution administrators. Send {\"status\": \"RESOLVED\"}; any other status is 400. "
                    + "Returns the resolved incident (200); 403 for other roles, 404 when it does not exist "
                    + "and 409 when it is already resolved."
    )
    @PatchMapping(
            value = "/{incidentId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DeviceIncidentResource> resolve(
            @PathVariable @Positive Long incidentId,
            @Valid @RequestBody UpdateDeviceIncidentResource resource
    ) {
        var actor = iam.getCurrentUser();

        if (actor == null ||
                actor.institutionId() == null ||
                actor.userId() == null) {
            throw new AccessDeniedException(
                    "Authenticated institutional user required."
            );
        }

        var resolved = commands.handle(
                DeviceIncidentCommandFromResourceAssembler
                        .toResolveCommand(
                                incidentId,
                                actor.userId(),
                                Instant.now(clock)
                        )
        );

        return ResponseEntity.ok(
                DeviceIncidentResourceFromEntityAssembler
                        .toResourceFromEntity(resolved)
        );
    }

    private static GetDeviceIncidentsQuery.Status statusFilter(String status) {
        if (status == null) {
            return null;
        }

        try {
            return GetDeviceIncidentsQuery.Status.valueOf(status);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "status must be OPEN or RESOLVED."
            );
        }
    }

    private Long currentInstitutionId() {
        var user = iam.getCurrentUser();

        if (user == null || user.institutionId() == null) {
            throw new AccessDeniedException(
                    "Institution-scoped user required."
            );
        }

        return user.institutionId();
    }
}

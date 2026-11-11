package com.upc.quadrapp.parkingsensing.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceIncidentsQuery;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentCommandService;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentQueryService;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.DeviceIncidentResource;
import com.upc.quadrapp.parkingsensing.interfaces.rest.resources.ReportDeviceIncidentResource;
import com.upc.quadrapp.parkingsensing.interfaces.rest.transform.DeviceIncidentCommandFromResourceAssembler;
import com.upc.quadrapp.parkingsensing.interfaces.rest.transform.DeviceIncidentResourceFromEntityAssembler;
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

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/device-incidents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Device Incidents", description = "Report, query, and resolve IoT device incidents.")
public class DeviceIncidentController {

    private final DeviceIncidentCommandService commands;
    private final DeviceIncidentQueryService queries;
    private final IamContextFacade iam;
    private final Clock clock;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceIncidentResource> report(
            @Valid @RequestBody ReportDeviceIncidentResource resource
    ) {
        var created = commands.handle(
                DeviceIncidentCommandFromResourceAssembler.toCommand(
                        currentInstitutionId(), resource
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                DeviceIncidentResourceFromEntityAssembler.toResourceFromEntity(created)
        );
    }

    @GetMapping
    public ResponseEntity<List<DeviceIncidentResource>> list(
            @RequestParam(required = false) @Positive Long parkingLotId,
            @RequestParam(defaultValue = "false") boolean onlyOpen
    ) {
        var incidents = queries.handle(new GetDeviceIncidentsQuery(
                currentInstitutionId(), parkingLotId, onlyOpen
        ));
        return ResponseEntity.ok(incidents.stream()
                .map(DeviceIncidentResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @PostMapping("/{incidentId}/resolve")
    public ResponseEntity<DeviceIncidentResource> resolve(
            @PathVariable @Positive Long incidentId
    ) {
        var actor = iam.getCurrentUser();
        if (actor == null || actor.institutionId() == null || actor.userId() == null) {
            throw new AccessDeniedException("Authenticated institutional user required.");
        }
        var resolved = commands.handle(
                DeviceIncidentCommandFromResourceAssembler.toResolveCommand(
                        incidentId, actor.userId(), Instant.now(clock)
                )
        );
        return ResponseEntity.ok(
                DeviceIncidentResourceFromEntityAssembler.toResourceFromEntity(resolved)
        );
    }

    private Long currentInstitutionId() {
        var user = iam.getCurrentUser();
        if (user == null || user.institutionId() == null) {
            throw new AccessDeniedException("Institution-scoped user required.");
        }
        return user.institutionId();
    }
}

package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;

import com.upc.quadrapp.parkingconfiguration.domain.model.queries.*;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;

import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Parking Lots", description = "Register and query institutional parking lots.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/parking-lots", produces = MediaType.APPLICATION_JSON_VALUE)
public class ParkingLotController {

    private final ParkingConfigurationCommandService commands;
    private final ParkingConfigurationQueryService queries;
    private final IamContextFacade iam;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParkingLotResource> register(
            @Valid @RequestBody RegisterParkingLotResource resource
    ) {
        var actor = iam.getCurrentUser();

        Long institutionId = requireInstitutionId(
                actor.institutionId()
        );

        var created = commands.handle(
                ParkingConfigurationCommandFromResourceAssembler
                        .toCommand(
                                institutionId,
                                actor.userId(),
                                resource
                        )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ParkingConfigurationResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    @GetMapping
    public ResponseEntity<List<ParkingLotResource>> list() {

        Long institutionId = requireInstitutionId(
                iam.getCurrentUser().institutionId()
        );

        var result = queries.handle(
                new GetParkingLotsByInstitutionQuery(institutionId)
        );

        return ResponseEntity.ok(
                result.stream()
                        .map(
                                ParkingConfigurationResourceFromEntityAssembler
                                        ::toResourceFromEntity
                        )
                        .toList()
        );
    }

    @GetMapping("/{parkingLotId}")
    public ResponseEntity<ParkingLotResource> getById(
            @PathVariable Long parkingLotId
    ) {
        requireInstitutionId(
                iam.getCurrentUser().institutionId()
        );

        var result = queries.handle(
                new GetParkingLotByIdQuery(parkingLotId)
        ).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parking lot not found."
                )
        );

        return ResponseEntity.ok(
                ParkingConfigurationResourceFromEntityAssembler
                        .toResourceFromEntity(result)
        );
    }

    private Long requireInstitutionId(Long institutionId) {

        if (institutionId == null || institutionId <= 0) {
            throw new AccessDeniedException(
                    "An institution-scoped user is required."
            );
        }

        return institutionId;
    }
}

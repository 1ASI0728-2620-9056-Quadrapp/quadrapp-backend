package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.*;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/parking-lots", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Parking Lots", description = "")
public class ParkingLotController {

    private final ParkingConfigurationCommandService commands;
    private final ParkingConfigurationQueryService queries;
    private final IamContextFacade iam;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParkingLotResource> register(
            @Valid @RequestBody RegisterParkingLotResource resource
    ) {
        Long institutionId = requireCurrentInstitutionId();

        var result = commands.handle(
                ParkingConfigurationCommandFromResourceAssembler
                        .toCommand(institutionId, resource)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ParkingConfigurationResourceFromEntityAssembler
                                .toResourceFromEntity(result)
                );
    }

    @GetMapping
    public ResponseEntity<List<ParkingLotResource>> list() {

        Long institutionId = requireCurrentInstitutionId();

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

    private Long requireCurrentInstitutionId() {

        Long institutionId = iam.getCurrentUser().institutionId();

        if (institutionId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "An institution-scoped user is required."
            );
        }

        return institutionId;
    }
}

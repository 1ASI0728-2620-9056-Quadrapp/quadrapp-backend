package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetAccessesByParkingLotQuery;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.AccessResource;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.CreateAccessResource;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.ParkingConfigurationCommandFromResourceAssembler;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.ParkingConfigurationResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/parking-lots/{parkingLotId}/accesses", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Parking Accesses", description = "Create and query the accesses of a parking lot.")
public class ParkingAccessController {

    private final ParkingConfigurationCommandService commands;
    private final ParkingConfigurationQueryService queries;

    @Operation(
            operationId = "createParkingAccess",
            summary = "Create an access of a parking lot",
            description = "Institution administrators. Adds an ENTRY, EXIT or BIDIRECTIONAL access to the "
                    + "parking lot (201); 400 for invalid data, 403 for other roles, 404 when the parking "
                    + "lot does not exist or belongs to another institution and 409 when the name already "
                    + "exists in the parking lot."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessResource> create(
            @PathVariable Long parkingLotId,
            @Valid @RequestBody CreateAccessResource resource
    ) {
        var created = commands.handle(
                ParkingConfigurationCommandFromResourceAssembler
                        .toCommand(parkingLotId, resource)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ParkingConfigurationResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    @Operation(
            operationId = "listParkingAccesses",
            summary = "List the accesses of a parking lot",
            description = "Any user of the institution that owns the parking lot. Returns its accesses (200); "
                    + "404 when the parking lot does not exist or belongs to another institution."
    )
    @GetMapping
    public ResponseEntity<List<AccessResource>> list(
            @PathVariable Long parkingLotId
    ) {
        var result = queries.handle(
                new GetAccessesByParkingLotQuery(parkingLotId)
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
}

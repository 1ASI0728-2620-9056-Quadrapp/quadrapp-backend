
package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.parkingconfiguration.domain.model.queries.*;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform.*;

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
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Parking Layout", description = "Create and query zones, levels, and parking spaces.")
public class ParkingLayoutController {

    private final ParkingConfigurationCommandService commands;
    private final ParkingConfigurationQueryService queries;

    @Operation(
            operationId = "createZone",
            summary = "Create a zone in a parking lot",
            description = "Institution administrators. Adds a zone to the parking lot (201); 400 for invalid "
                    + "data, 403 for other roles, 404 when the parking lot does not exist or belongs to "
                    + "another institution and 409 when the name already exists in the parking lot."
    )
    @PostMapping(
            value = "/api/v1/parking-lots/{parkingLotId}/zones",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ZoneResource> createZone(
            @PathVariable Long parkingLotId,
            @Valid @RequestBody CreateZoneResource resource
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
            operationId = "listZones",
            summary = "List the zones of a parking lot",
            description = "Any user of the institution that owns the parking lot. Returns its zones (200); 404 "
                    + "when the parking lot does not exist or belongs to another institution."
    )
    @GetMapping("/api/v1/parking-lots/{parkingLotId}/zones")
    public ResponseEntity<List<ZoneResource>> listZones(
            @PathVariable Long parkingLotId
    ) {
        var result = queries.handle(
                new GetZonesByParkingLotQuery(parkingLotId)
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

    @Operation(
            operationId = "createLevel",
            summary = "Create a level in a zone",
            description = "Institution administrators. Adds a level to the zone (201); 400 for invalid data, "
                    + "403 for other roles, 404 when the zone does not exist or belongs to another "
                    + "institution and 409 when the name already exists in the zone."
    )
    @PostMapping(
            value = "/api/v1/zones/{zoneId}/levels",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LevelResource> createLevel(
            @PathVariable Long zoneId,
            @Valid @RequestBody CreateLevelResource resource
    ) {
        var created = commands.handle(
                ParkingConfigurationCommandFromResourceAssembler
                        .toCommand(zoneId, resource)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ParkingConfigurationResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    @Operation(
            operationId = "listLevels",
            summary = "List the levels of a zone",
            description = "Any user of the institution that owns the zone. Returns its levels (200); 404 when "
                    + "the zone does not exist or belongs to another institution."
    )
    @GetMapping("/api/v1/zones/{zoneId}/levels")
    public ResponseEntity<List<LevelResource>> listLevels(
            @PathVariable Long zoneId
    ) {
        var result = queries.handle(
                new GetLevelsByZoneQuery(zoneId)
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

    @Operation(
            operationId = "createSpace",
            summary = "Create a parking space in a level",
            description = "Institution administrators. Adds a parking space to the level (201); 400 for invalid "
                    + "data, 403 for other roles, 404 when the level does not exist or belongs to another "
                    + "institution and 409 when the label already exists in the level."
    )
    @PostMapping(
            value = "/api/v1/levels/{levelId}/spaces",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SpaceResource> createSpace(
            @PathVariable Long levelId,
            @Valid @RequestBody CreateSpaceResource resource
    ) {
        var created = commands.handle(
                ParkingConfigurationCommandFromResourceAssembler
                        .toCommand(levelId, resource)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ParkingConfigurationResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    @Operation(
            operationId = "listSpaces",
            summary = "List the parking spaces of a level",
            description = "Any user of the institution that owns the level. Returns its spaces (200); 404 when "
                    + "the level does not exist or belongs to another institution."
    )
    @GetMapping("/api/v1/levels/{levelId}/spaces")
    public ResponseEntity<List<SpaceResource>> listSpaces(
            @PathVariable Long levelId
    ) {
        var result = queries.handle(
                new GetSpacesByLevelQuery(levelId)
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

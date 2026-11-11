
package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Parking Layout", description = "")
public class ParkingLayoutController {

    private final ParkingConfigurationCommandService commands;
    private final ParkingConfigurationQueryService queries;

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

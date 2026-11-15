package com.upc.quadrapp.occupancy.interfaces.rest;

import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import com.upc.quadrapp.occupancy.interfaces.rest.resources.OccupancySnapshotResource;
import com.upc.quadrapp.occupancy.interfaces.rest.resources.OccupancySummaryResource;
import com.upc.quadrapp.occupancy.interfaces.rest.resources.SpaceStatusResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Occupancy", description = "Current occupancy and historical snapshots")
public class OccupancyController {

    private final OccupancyCommandService commands;
    private final OccupancyQueryService queries;

    @Operation(
            operationId = "getLotOccupancy",
            summary = "Get the current occupancy of a parking lot",
            description = "Any user of the institution that owns the parking lot. Returns the current space "
                    + "counts by status (200); 404 when the parking lot does not exist or belongs to "
                    + "another institution."
    )
    @GetMapping("/api/v1/parking-lots/{parkingLotId}/occupancy")
    public ResponseEntity<OccupancySummaryResource> getByParkingLot(
            @PathVariable Long parkingLotId
    ) {
        return ResponseEntity.ok(
                OccupancySummaryResource.from(queries.getByParkingLot(parkingLotId))
        );
    }

    @Operation(
            operationId = "listLotSpaceStatuses",
            summary = "List the current status of each space of a parking lot",
            description = "Any user of the institution that owns the parking lot. Returns every active space "
                    + "with its current status, ordered by spaceId (200); 404 when the parking lot does not "
                    + "exist or belongs to another institution."
    )
    @GetMapping("/api/v1/parking-lots/{parkingLotId}/space-statuses")
    public ResponseEntity<List<SpaceStatusResource>> getSpaceStatuses(
            @PathVariable Long parkingLotId
    ) {
        return ResponseEntity.ok(
                queries.getSpaceStatuses(parkingLotId)
                        .stream()
                        .map(SpaceStatusResource::from)
                        .toList()
        );
    }

    @Operation(
            operationId = "getZoneOccupancy",
            summary = "Get the current occupancy of a zone",
            description = "Any user of the institution that owns the zone. Returns the current space counts by "
                    + "status (200); 404 when the zone does not exist or belongs to another institution."
    )
    @GetMapping("/api/v1/zones/{zoneId}/occupancy")
    public ResponseEntity<OccupancySummaryResource> getByZone(
            @PathVariable Long zoneId
    ) {
        return ResponseEntity.ok(
                OccupancySummaryResource.from(queries.getByZone(zoneId))
        );
    }

    @Operation(
            operationId = "getLevelOccupancy",
            summary = "Get the current occupancy of a level",
            description = "Any user of the institution that owns the level. Returns the current space counts by "
                    + "status (200); 404 when the level does not exist or belongs to another institution."
    )
    @GetMapping("/api/v1/levels/{levelId}/occupancy")
    public ResponseEntity<OccupancySummaryResource> getByLevel(
            @PathVariable Long levelId
    ) {
        return ResponseEntity.ok(
                OccupancySummaryResource.from(queries.getByLevel(levelId))
        );
    }

    @Operation(
            operationId = "createZoneOccupancySnapshot",
            summary = "Record an occupancy snapshot of a zone",
            description = "Any user of the institution that owns the zone. Stores the current counts of the "
                    + "zone as a snapshot and returns it (201); 404 when the zone does not exist or belongs "
                    + "to another institution."
    )
    @PostMapping("/api/v1/zones/{zoneId}/occupancy-snapshots")
    public ResponseEntity<OccupancySnapshotResource> createSnapshot(
            @PathVariable Long zoneId
    ) {
        var snapshot = commands.handle(
                new CreateOccupancySnapshotCommand(zoneId)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OccupancySnapshotResource.from(snapshot));
    }

    @Operation(
            operationId = "listZoneOccupancySnapshots",
            summary = "List the occupancy snapshots of a zone",
            description = "Any user of the institution that owns the zone. Returns the snapshots between from "
                    + "and to (ISO-8601), or of the last 24 hours when both are omitted (200); 400 when "
                    + "only one bound is given or from is after to, and 404 when the zone does not exist or "
                    + "belongs to another institution."
    )
    @GetMapping("/api/v1/zones/{zoneId}/occupancy-snapshots")
    public ResponseEntity<List<OccupancySnapshotResource>> getSnapshots(
            @PathVariable Long zoneId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return ResponseEntity.ok(
                queries.getSnapshots(zoneId, from, to)
                        .stream()
                        .map(OccupancySnapshotResource::from)
                        .toList()
        );
    }
}

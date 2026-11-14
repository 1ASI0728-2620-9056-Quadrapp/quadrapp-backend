package com.upc.quadrapp.occupancy.interfaces.rest;

import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import com.upc.quadrapp.occupancy.interfaces.rest.resources.OccupancySnapshotResource;
import com.upc.quadrapp.occupancy.interfaces.rest.resources.OccupancySummaryResource;
import com.upc.quadrapp.occupancy.interfaces.rest.resources.SpaceStatusResource;
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

    @GetMapping("/api/v1/parking-lots/{parkingLotId}/occupancy")
    public ResponseEntity<OccupancySummaryResource> getByParkingLot(
            @PathVariable Long parkingLotId
    ) {
        return ResponseEntity.ok(
                OccupancySummaryResource.from(queries.getByParkingLot(parkingLotId))
        );
    }

    @GetMapping("/api/v1/parking-lots/{parkingLotId}/spaces/status")
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

    @GetMapping("/api/v1/zones/{zoneId}/occupancy")
    public ResponseEntity<OccupancySummaryResource> getByZone(
            @PathVariable Long zoneId
    ) {
        return ResponseEntity.ok(
                OccupancySummaryResource.from(queries.getByZone(zoneId))
        );
    }

    @GetMapping("/api/v1/levels/{levelId}/occupancy")
    public ResponseEntity<OccupancySummaryResource> getByLevel(
            @PathVariable Long levelId
    ) {
        return ResponseEntity.ok(
                OccupancySummaryResource.from(queries.getByLevel(levelId))
        );
    }

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

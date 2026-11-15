package com.upc.quadrapp.parkingsensing.interfaces.rest;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.queries.*;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.HardwareIdentifier;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceCommandService;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceQueryService;

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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Devices", description = "Register, query, assign, and decommission IoT devices.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/devices", produces = MediaType.APPLICATION_JSON_VALUE)
public class DeviceController {

    private final DeviceCommandService commands;
    private final DeviceQueryService queries;
    private final IamContextFacade iam;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceResource> register(
            @Valid @RequestBody RegisterDeviceResource resource
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
                DeviceCommandFromResourceAssembler.toCommand(
                        actor.institutionId(),
                        actor.userId(),
                        resource
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        DeviceResourceFromEntityAssembler
                                .toResourceFromEntity(created)
                );
    }

    @Operation(summary = "List devices filtered by parkingLotId, by hardwareId (0 or 1 device) or by both, "
            + "which then apply together; at least one filter is required")
    @GetMapping
    public ResponseEntity<List<DeviceResource>> list(
            @RequestParam(required = false) @Positive Long parkingLotId,
            @RequestParam(required = false) String hardwareId
    ) {
        if (parkingLotId == null && hardwareId == null) {
            throw new IllegalArgumentException(
                    "parkingLotId or hardwareId is required."
            );
        }

        List<Device> devices;

        if (parkingLotId == null) {
            devices = queries.handle(
                    new GetDeviceByHardwareIdQuery(hardwareId)
            ).stream().toList();
        } else {
            var inParkingLot = queries.handle(
                    new GetDevicesByParkingLotQuery(
                            currentInstitutionId(),
                            parkingLotId
                    )
            );

            devices = hardwareId == null
                    ? inParkingLot
                    : withHardwareId(inParkingLot, hardwareId);
        }

        return ResponseEntity.ok(
                devices.stream()
                        .map(
                                DeviceResourceFromEntityAssembler
                                        ::toResourceFromEntity
                        )
                        .toList()
        );
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceResource> getById(
            @PathVariable @Positive Long deviceId
    ) {
        var device = queries.handle(
                new GetDeviceByIdQuery(deviceId)
        ).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Device not found."
                )
        );

        return ResponseEntity.ok(
                DeviceResourceFromEntityAssembler
                        .toResourceFromEntity(device)
        );
    }

    @PutMapping(
            value = "/{deviceId}/assignment",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DeviceResource> assign(
            @PathVariable @Positive Long deviceId,
            @Valid @RequestBody AssignDeviceResource resource
    ) {
        var updated = commands.handle(
                DeviceCommandFromResourceAssembler.toCommand(
                        deviceId,
                        resource
                )
        );

        return ResponseEntity.ok(
                DeviceResourceFromEntityAssembler
                        .toResourceFromEntity(updated)
        );
    }

    @Operation(summary = "Remove a device logically: it is decommissioned and can still be read with GET")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<DeviceResource> decommission(
            @PathVariable @Positive Long deviceId
    ) {
        var updated = commands.handle(
                DeviceCommandFromResourceAssembler
                        .toDecommissionCommand(deviceId)
        );

        return ResponseEntity.ok(
                DeviceResourceFromEntityAssembler
                        .toResourceFromEntity(updated)
        );
    }

    private static List<Device> withHardwareId(List<Device> devices, String hardwareId) {
        String wanted = new HardwareIdentifier(hardwareId).value();

        return devices.stream()
                .filter(device -> device.getHardwareId().equals(wanted))
                .toList();
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

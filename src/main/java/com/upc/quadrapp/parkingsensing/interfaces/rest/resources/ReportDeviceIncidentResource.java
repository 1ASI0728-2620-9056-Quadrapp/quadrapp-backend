package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record ReportDeviceIncidentResource(
        @Positive Long parkingLotId,
        @Positive Long deviceId,
        @NotBlank String hardwareId,
        @NotNull IncidentType type,
        String details,
        @NotNull Instant occurredAt
) {}
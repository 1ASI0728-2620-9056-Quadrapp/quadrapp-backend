package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterDeviceResource(
        @NotNull @Positive Long parkingLotId,
        @NotBlank String hardwareId,
        @NotNull DeviceType type,
        @Positive int reportIntervalSeconds
) {}
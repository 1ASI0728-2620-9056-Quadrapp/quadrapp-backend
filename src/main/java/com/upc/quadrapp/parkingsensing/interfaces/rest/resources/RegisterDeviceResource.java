package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;

import jakarta.validation.constraints.*;

public record RegisterDeviceResource(@NotNull @Positive Long parkingLotId,
                                     @NotBlank @Size(max = 80) String hardwareId,
                                     @NotNull DeviceType type,
                                     @Positive int reportIntervalSeconds,
                                     @Positive Long gatewayId,
                                     Boolean directional,
                                     @Size(max = 30) String firmwareVersion) {}

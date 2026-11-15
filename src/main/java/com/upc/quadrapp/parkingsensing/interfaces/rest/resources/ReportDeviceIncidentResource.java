package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentCategory;

import jakarta.validation.constraints.*;

public record ReportDeviceIncidentResource(@NotNull @Positive Long parkingLotId,
                                           @Positive Long deviceId,
                                           @Size(max = 80) String hardwareId,
                                           @NotNull IncidentCategory category,
                                           String details) {}

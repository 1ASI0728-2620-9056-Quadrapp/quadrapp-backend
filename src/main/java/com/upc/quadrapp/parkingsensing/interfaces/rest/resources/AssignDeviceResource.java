package com.upc.quadrapp.parkingsensing.interfaces.rest.resources;

import jakarta.validation.constraints.Positive;

public record AssignDeviceResource(
        @Positive Long spaceId,
        @Positive Long accessId
) {}
package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateZoneResource(@NotBlank @Size(max = 60) String name,
        @Size(max = 80) String description, Short sortOrder) {}

package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.AccessType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccessResource(@NotBlank @Size(max = 60) String name,
                                   @NotNull AccessType type,
                                   Boolean flowCountingEnabled) {}

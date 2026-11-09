package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateSpaceResource(@NotBlank String label) {}

package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSpaceResource(@NotBlank @Size(max = 20) String label,
        SpaceType type, @Digits(integer = 5, fraction = 2) BigDecimal posX,
        @Digits(integer = 5, fraction = 2) BigDecimal posY) {}

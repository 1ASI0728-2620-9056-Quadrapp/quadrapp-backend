package com.upc.quadrapp.iam.interfaces.rest.resources;

import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InviteUserResource(@NotNull @Positive Long institutionId,
                                 @NotBlank @Email String email,
                                 @NotNull Roles role) {}

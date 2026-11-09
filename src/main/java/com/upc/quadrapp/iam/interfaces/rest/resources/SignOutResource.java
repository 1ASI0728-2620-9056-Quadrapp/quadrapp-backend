package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record SignOutResource(@NotBlank String refreshToken) {}

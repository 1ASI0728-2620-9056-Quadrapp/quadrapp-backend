package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignInResource(@NotBlank @Email String email, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otpCode) {}

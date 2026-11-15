package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestOtpResource(@NotBlank @Email String email) {}

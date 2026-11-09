package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInstitutionResource(@NotBlank @Size(max = 150) String name) {}

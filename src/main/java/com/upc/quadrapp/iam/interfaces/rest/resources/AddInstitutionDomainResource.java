package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddInstitutionDomainResource(@NotBlank @Size(max = 120) String domain) {}

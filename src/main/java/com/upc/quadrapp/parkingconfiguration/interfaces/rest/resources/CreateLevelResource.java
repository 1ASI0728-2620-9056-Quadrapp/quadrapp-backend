
package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLevelResource(@NotBlank @Size(max = 40) String name, Short sortOrder) {}

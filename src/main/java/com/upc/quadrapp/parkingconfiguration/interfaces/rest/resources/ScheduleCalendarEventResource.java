package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleCalendarEventResource(Long parkingLotId, Long zoneId,
                                            @NotBlank String name,
                                            @NotNull Instant startAt, @NotNull Instant endAt) {}

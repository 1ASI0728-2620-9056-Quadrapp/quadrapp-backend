package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.CalendarEventType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventImpact;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record ScheduleCalendarEventResource(Long parkingLotId, Long zoneId,
                                            @NotBlank @Size(max = 120) String name,
                                            @NotNull Instant startAt,
                                            @NotNull Instant endAt,
                                            @NotNull CalendarEventType type,
                                            EventImpact impact) {}

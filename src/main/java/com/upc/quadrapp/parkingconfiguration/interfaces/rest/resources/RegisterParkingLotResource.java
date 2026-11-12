package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record RegisterParkingLotResource(@NotBlank @Size(max = 120) String name,
                                         @Size(max = 255) String address,
                                         @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                                         @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                                         LocalTime opensAt, LocalTime closesAt,
                                         @Min(1) @Max(100) Integer saturationThresholdPct,
                                         @Min(1) @Max(100) Integer lowBatteryThresholdPct,
                                         @Positive Integer staleAfterSeconds,
                                         @Positive Integer flowWindowMinutes,
                                         @Min(1) @Max(100) Integer accuracyMarginPct,
                                         @Positive Integer occupiedDebounceS,
                                         @Positive Integer freedDebounceS) {}

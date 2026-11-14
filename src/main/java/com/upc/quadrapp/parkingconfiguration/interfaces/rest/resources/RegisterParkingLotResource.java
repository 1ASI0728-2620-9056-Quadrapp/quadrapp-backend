package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record RegisterParkingLotResource(@NotBlank @Size(max = 120) String name,
                                         @Size(max = 255) String address,
                                         @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                                         @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                                         LocalTime opensAt, LocalTime closesAt,
                                         @Min(50) @Max(100) Integer saturationThresholdPct,
                                         @Min(1) @Max(100) Integer lowBatteryThresholdPct,
                                         @Positive Integer staleAfterSeconds,
                                         @Min(5) @Max(60) Integer flowWindowMinutes,
                                         @Min(1) @Max(30) Integer accuracyMarginPct,
                                         @Min(3) @Max(60) Integer occupiedDebounceS,
                                         @Min(3) @Max(60) Integer freedDebounceS) {}

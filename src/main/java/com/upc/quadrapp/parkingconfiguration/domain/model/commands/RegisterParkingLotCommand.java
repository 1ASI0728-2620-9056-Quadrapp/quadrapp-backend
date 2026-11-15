package com.upc.quadrapp.parkingconfiguration.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalTime;

public record RegisterParkingLotCommand(Long institutionId, Long createdBy, String name, String address,
        BigDecimal latitude, BigDecimal longitude, LocalTime opensAt, LocalTime closesAt, Integer saturationThresholdPct,
        Integer lowBatteryThresholdPct, Integer staleAfterSeconds, Integer flowWindowMinutes, Integer accuracyMarginPct,
        Integer occupiedDebounceS, Integer freedDebounceS, Integer lowAvailabilityPct)
{
    public RegisterParkingLotCommand {
        if (institutionId == null || institutionId <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("institutionId and name are required.");
        }
    }
}

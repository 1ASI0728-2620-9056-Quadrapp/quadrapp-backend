package com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

public record ParkingLotResource(Long id, Long institutionId, Long createdBy, String name, String address,
        BigDecimal latitude, BigDecimal longitude, LocalTime opensAt, LocalTime closesAt,
        int saturationThresholdPct, int lowBatteryThresholdPct, int staleAfterSeconds, int flowWindowMinutes,
        int accuracyMarginPct, int occupiedDebounceS, int freedDebounceS, int lowAvailabilityPct, int layoutVersion,
        Instant layoutPublishedAt, boolean active, Instant createdAt, Instant updatedAt) {}

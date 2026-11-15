
package com.upc.quadrapp.parkingconfiguration.domain.model.aggregates;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

public final class ParkingLot {

    private final Long id;
    private final Long institutionId;
    private final Long createdBy;

    private final String name;
    private final String address;

    private final BigDecimal latitude;
    private final BigDecimal longitude;

    private final LocalTime opensAt;
    private final LocalTime closesAt;

    private final int saturationThresholdPct;
    private final int lowBatteryThresholdPct;
    private final int staleAfterSeconds;
    private final int flowWindowMinutes;
    private final int accuracyMarginPct;
    private final int occupiedDebounceS;
    private final int freedDebounceS;
    private final int lowAvailabilityPct;
    private final int layoutVersion;

    private final Instant layoutPublishedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private final boolean active;

    private ParkingLot(
            Long id,
            Long institutionId,
            Long createdBy,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalTime opensAt,
            LocalTime closesAt,
            int saturationThresholdPct,
            int lowBatteryThresholdPct,
            int staleAfterSeconds,
            int flowWindowMinutes,
            int accuracyMarginPct,
            int occupiedDebounceS,
            int freedDebounceS,
            int lowAvailabilityPct,
            int layoutVersion,
            Instant layoutPublishedAt,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    "id must be positive."
            );
        }

        if (institutionId == null || institutionId <= 0) {
            throw new IllegalArgumentException(
                    "institutionId must be positive."
            );
        }

        if ((opensAt == null) != (closesAt == null)) {
            throw new IllegalArgumentException(
                    "Both opening and closing times are required together."
            );
        }

        if (latitude != null &&
                (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 ||
                        latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new IllegalArgumentException(
                    "latitude out of range."
            );
        }

        if (longitude != null &&
                (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 ||
                        longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new IllegalArgumentException(
                    "longitude out of range."
            );
        }

        // Ranges of OperatingSettings in the report: saturation 50-100 %, flow window 5-60 min,
        // minimum detection time 3-60 s, accuracy margin 1-30 points and low availability 5-30 %.
        if (outside(saturationThresholdPct, 50, 100) ||
                outside(lowBatteryThresholdPct, 1, 100) ||
                outside(accuracyMarginPct, 1, 30) ||
                staleAfterSeconds <= 0 ||
                outside(flowWindowMinutes, 5, 60) ||
                outside(occupiedDebounceS, 3, 60) ||
                outside(freedDebounceS, 3, 60) ||
                outside(lowAvailabilityPct, 5, 30) ||
                layoutVersion < 0) {
            throw new IllegalArgumentException(
                    "Invalid parking lot settings."
            );
        }

        this.id = id;
        this.institutionId = institutionId;
        this.createdBy = createdBy;

        this.name = new ConfigurationName(name).value();
        this.address = address;

        this.latitude = latitude;
        this.longitude = longitude;

        this.opensAt = opensAt;
        this.closesAt = closesAt;

        this.saturationThresholdPct = saturationThresholdPct;
        this.lowBatteryThresholdPct = lowBatteryThresholdPct;
        this.staleAfterSeconds = staleAfterSeconds;
        this.flowWindowMinutes = flowWindowMinutes;
        this.accuracyMarginPct = accuracyMarginPct;
        this.occupiedDebounceS = occupiedDebounceS;
        this.freedDebounceS = freedDebounceS;
        this.lowAvailabilityPct = lowAvailabilityPct;

        this.layoutVersion = layoutVersion;
        this.layoutPublishedAt = layoutPublishedAt;
        this.active = active;

        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static boolean outside(int value, int min, int max) {
        return value < min || value > max;
    }

    public static ParkingLot register(
            Long institutionId,
            Long createdBy,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalTime opensAt,
            LocalTime closesAt,
            int saturation,
            int lowBattery,
            int stale,
            int flow,
            int accuracy,
            int occupied,
            int freed,
            int lowAvailability
    ) {
        var now = Instant.now();

        return new ParkingLot(
                null,
                institutionId,
                createdBy,
                name,
                address,
                latitude,
                longitude,
                opensAt,
                closesAt,
                saturation,
                lowBattery,
                stale,
                flow,
                accuracy,
                occupied,
                freed,
                lowAvailability,
                0,
                null,
                true,
                now,
                now
        );
    }

    public static ParkingLot restore(
            Long id,
            Long institutionId,
            Long createdBy,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalTime opensAt,
            LocalTime closesAt,
            int saturation,
            int lowBattery,
            int stale,
            int flow,
            int accuracy,
            int occupied,
            int freed,
            int lowAvailability,
            int layoutVersion,
            Instant publishedAt,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ParkingLot(
                id,
                institutionId,
                createdBy,
                name,
                address,
                latitude,
                longitude,
                opensAt,
                closesAt,
                saturation,
                lowBattery,
                stale,
                flow,
                accuracy,
                occupied,
                freed,
                lowAvailability,
                layoutVersion,
                publishedAt,
                active,
                createdAt,
                updatedAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public int getSaturationThresholdPct() {
        return saturationThresholdPct;
    }

    public int getLowBatteryThresholdPct() {
        return lowBatteryThresholdPct;
    }

    public int getStaleAfterSeconds() {
        return staleAfterSeconds;
    }

    public int getFlowWindowMinutes() {
        return flowWindowMinutes;
    }

    public int getAccuracyMarginPct() {
        return accuracyMarginPct;
    }

    public int getOccupiedDebounceS() {
        return occupiedDebounceS;
    }

    public int getFreedDebounceS() {
        return freedDebounceS;
    }

    public int getLowAvailabilityPct() {
        return lowAvailabilityPct;
    }

    public int getLayoutVersion() {
        return layoutVersion;
    }

    public Instant getLayoutPublishedAt() {
        return layoutPublishedAt;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

package com.upc.quadrapp.parkingsensing.domain.model.aggregates;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.Objects;

public final class Device {

    private final Long id;
    private final Long institutionId;
    private final Long parkingLotId;
    private final Long registeredBy;

    private final HardwareIdentifier hardwareId;
    private final DeviceType type;
    private final int reportIntervalSeconds;

    private DeviceStatus status;

    private Long gatewayId;
    private Long spaceId;
    private Long accessId;
    private Long replacedBy;

    private String apiKeyHash;
    private String firmwareVersion;

    private Boolean directional;
    private Integer batteryPct;

    private Instant lastSeenAt;
    private Instant decommissionedAt;
    private Instant updatedAt;

    private final Instant registeredAt;

    private Device(
            Long id,
            Long institutionId,
            Long parkingLotId,
            String hardwareId,
            DeviceType type,
            int reportIntervalSeconds,
            DeviceStatus status,
            Long gatewayId,
            Long spaceId,
            Long accessId,
            Long replacedBy,
            Long registeredBy,
            String apiKeyHash,
            Boolean directional,
            Integer batteryPct,
            String firmwareVersion,
            Instant lastSeenAt,
            Instant decommissionedAt,
            Instant registeredAt,
            Instant updatedAt
    ) {
        requirePositive(institutionId, "institutionId");
        requirePositive(parkingLotId, "parkingLotId");

        if (id != null) {
            requirePositive(id, "id");
        }

        if (reportIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Report interval must be positive."
            );
        }

        this.id = id;
        this.institutionId = institutionId;
        this.parkingLotId = parkingLotId;
        this.hardwareId = new HardwareIdentifier(hardwareId);
        this.type = Objects.requireNonNull(
                type, "Device type is required."
        );
        this.status = Objects.requireNonNull(
                status, "Device status is required."
        );

        this.reportIntervalSeconds = reportIntervalSeconds;

        this.gatewayId = gatewayId;
        this.spaceId = spaceId;
        this.accessId = accessId;
        this.replacedBy = replacedBy;
        this.registeredBy = registeredBy;

        this.apiKeyHash = apiKeyHash;
        this.directional = directional;
        this.batteryPct = batteryPct;
        this.firmwareVersion = firmwareVersion;

        this.lastSeenAt = lastSeenAt;
        this.decommissionedAt = decommissionedAt;
        this.registeredAt = Objects.requireNonNull(
                registeredAt, "Registration date is required."
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt, "Update date is required."
        );

        validateConfiguration();
    }

    public static Device register(
            Long institutionId,
            Long parkingLotId,
            String hardwareId,
            DeviceType type,
            int reportIntervalSeconds,
            Long gatewayId,
            Boolean directional,
            String firmwareVersion,
            Long registeredBy,
            Instant now
    ) {
        Objects.requireNonNull(now, "Registration time is required.");

        return new Device(
                null,
                institutionId,
                parkingLotId,
                hardwareId,
                type,
                reportIntervalSeconds,
                DeviceStatus.OPERATIONAL,
                gatewayId,
                null,
                null,
                null,
                registeredBy,
                null,
                type == DeviceType.PASS_SENSOR
                        ? (directional == null ? Boolean.FALSE : directional)
                        : directional,
                null,
                firmwareVersion,
                null,
                null,
                now,
                now
        );
    }

    public static Device restore(
            Long id,
            Long institutionId,
            Long parkingLotId,
            String hardwareId,
            DeviceType type,
            int reportIntervalSeconds,
            DeviceStatus status,
            Long gatewayId,
            Long spaceId,
            Long accessId,
            Long replacedBy,
            Long registeredBy,
            String apiKeyHash,
            Boolean directional,
            Integer batteryPct,
            String firmwareVersion,
            Instant lastSeenAt,
            Instant decommissionedAt,
            Instant registeredAt,
            Instant updatedAt
    ) {
        return new Device(
                id,
                institutionId,
                parkingLotId,
                hardwareId,
                type,
                reportIntervalSeconds,
                status,
                gatewayId,
                spaceId,
                accessId,
                replacedBy,
                registeredBy,
                apiKeyHash,
                directional,
                batteryPct,
                firmwareVersion,
                lastSeenAt,
                decommissionedAt,
                registeredAt,
                updatedAt
        );
    }

    public void assignToSpace(Long spaceId) {
        requireActive();

        if (type != DeviceType.SPACE_SENSOR) {
            throw new IllegalStateException(
                    "Only SPACE_SENSOR can be assigned to a space."
            );
        }

        requirePositive(spaceId, "spaceId");

        this.spaceId = spaceId;
        this.accessId = null;

        touch();
    }

    public void assignToAccess(Long accessId) {
        requireActive();

        if (type != DeviceType.PASS_SENSOR) {
            throw new IllegalStateException(
                    "Only PASS_SENSOR can be assigned to an access."
            );
        }

        requirePositive(accessId, "accessId");

        this.accessId = accessId;
        this.spaceId = null;

        touch();
    }

    public void decommission() {
        requireActive();

        this.status = DeviceStatus.DECOMMISSIONED;
        this.decommissionedAt = Instant.now();

        this.spaceId = null;
        this.accessId = null;

        touch();
    }

    public boolean recordReport(Instant readingAt) {
        requireActive();

        Objects.requireNonNull(
                readingAt, "Reading time is required."
        );

        if (lastSeenAt != null &&
                !readingAt.isAfter(lastSeenAt)) {
            return false;
        }

        lastSeenAt = readingAt;
        touch();

        return true;
    }

    public boolean isOverdue(Instant now) {
        Objects.requireNonNull(now);

        Instant reference = lastSeenAt == null
                ? registeredAt
                : lastSeenAt;

        return status != DeviceStatus.DECOMMISSIONED &&
                now.isAfter(
                        reference.plusSeconds(reportIntervalSeconds)
                );
    }

    private void validateConfiguration() {
        if (spaceId != null && accessId != null) {
            throw new IllegalArgumentException(
                    "Device cannot target a space and an access simultaneously."
            );
        }

        if (type == DeviceType.GATEWAY &&
                (spaceId != null || accessId != null)) {
            throw new IllegalArgumentException(
                    "Gateway cannot be assigned to a space or access."
            );
        }

        if (type == DeviceType.SPACE_SENSOR &&
                accessId != null) {
            throw new IllegalArgumentException(
                    "SPACE_SENSOR cannot target an access."
            );
        }

        if (type == DeviceType.PASS_SENSOR) {
            if (spaceId != null) {
                throw new IllegalArgumentException(
                        "PASS_SENSOR cannot target a parking space."
                );
            }

            if (directional == null) {
                throw new IllegalArgumentException(
                        "PASS_SENSOR requires a directional flag."
                );
            }
        }

        if (batteryPct != null &&
                (batteryPct < 0 || batteryPct > 100)) {
            throw new IllegalArgumentException(
                    "Battery percentage must be between 0 and 100."
            );
        }

        if (firmwareVersion != null &&
                firmwareVersion.length() > 30) {
            throw new IllegalArgumentException(
                    "Firmware version is too long."
            );
        }
    }

    private void requireActive() {
        if (status == DeviceStatus.DECOMMISSIONED) {
            throw new IllegalStateException(
                    "Device is decommissioned."
            );
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    field + " must be positive."
            );
        }
    }

    public Long getId() { return id; }
    public Long getInstitutionId() { return institutionId; }
    public Long getParkingLotId() { return parkingLotId; }
    public Long getRegisteredBy() { return registeredBy; }

    public String getHardwareId() { return hardwareId.value(); }
    public DeviceType getType() { return type; }
    public DeviceStatus getStatus() { return status; }

    public int getReportIntervalSeconds() {
        return reportIntervalSeconds;
    }

    public Long getGatewayId() { return gatewayId; }
    public Long getSpaceId() { return spaceId; }
    public Long getAccessId() { return accessId; }
    public Long getReplacedBy() { return replacedBy; }

    public String getApiKeyHash() { return apiKeyHash; }
    public Boolean getDirectional() { return directional; }
    public Integer getBatteryPct() { return batteryPct; }
    public String getFirmwareVersion() { return firmwareVersion; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getDecommissionedAt() { return decommissionedAt; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

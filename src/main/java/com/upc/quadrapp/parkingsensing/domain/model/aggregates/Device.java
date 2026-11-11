package com.upc.quadrapp.parkingsensing.domain.model.aggregates;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import java.time.Instant;
import java.util.Objects;

public final class Device {
    private final Long id;
    private final Long institutionId;
    private final Long parkingLotId;
    private final HardwareIdentifier hardwareId;
    private final DeviceType type;
    private final int reportIntervalSeconds;
    private DeviceStatus status;
    private Long spaceId;
    private Long accessId;
    private Instant lastSeenAt;
    private final Instant registeredAt;

    private Device(Long id, Long institutionId, Long parkingLotId,
                   String hardwareId, DeviceType type, int reportIntervalSeconds,
                   DeviceStatus status, Long spaceId, Long accessId, Instant lastSeenAt, Instant registeredAt) {
        if (id != null) positive(id, "id");
        positive(institutionId, "institutionId");
        positive(parkingLotId, "parkingLotId");
        if (reportIntervalSeconds <= 0) {
            throw new IllegalArgumentException("reportIntervalSeconds must be positive.");
        }
        this.id = id;
        this.institutionId = institutionId;
        this.parkingLotId = parkingLotId;
        this.hardwareId = new HardwareIdentifier(hardwareId);
        this.type = Objects.requireNonNull(type, "type is required.");
        this.reportIntervalSeconds = reportIntervalSeconds;
        this.status = Objects.requireNonNull(status, "status is required.");
        this.spaceId = spaceId;
        this.accessId = accessId;
        this.lastSeenAt = lastSeenAt;
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt is required.");
        if (spaceId != null && accessId != null) {
            throw new IllegalArgumentException("Device cannot target space and access together.");
        }
        if (spaceId != null && type != DeviceType.SPACE_SENSOR) {
            throw new IllegalArgumentException("Only SPACE_SENSOR can be assigned to a space.");
        }
        if (accessId != null && type != DeviceType.PASS_SENSOR) {
            throw new IllegalArgumentException("Only PASS_SENSOR can be assigned to an access.");
        }
        if (spaceId != null) positive(spaceId, "spaceId");
        if (accessId != null) positive(accessId, "accessId");
        if (status != DeviceStatus.ASSIGNED && (spaceId != null || accessId != null)) {
            throw new IllegalArgumentException("Only assigned devices can have targets.");
        }
        if (status == DeviceStatus.ASSIGNED && spaceId == null && accessId == null) {
            throw new IllegalArgumentException("Assigned device requires a target.");
        }
    }

    public static Device register(Long institutionId, Long parkingLotId,
                                  String hardwareId, DeviceType type, int reportIntervalSeconds, Instant registeredAt) {
        return new Device(null, institutionId, parkingLotId, hardwareId, type,
                reportIntervalSeconds, DeviceStatus.REGISTERED, null, null, null, registeredAt);
    }

    public static Device restore(Long id, Long institutionId, Long parkingLotId,
                                 String hardwareId, DeviceType type, int reportIntervalSeconds,
                                 DeviceStatus status, Long spaceId, Long accessId, Instant lastSeenAt, Instant registeredAt) {
        positive(id, "id");
        return new Device(id, institutionId, parkingLotId, hardwareId, type,
                reportIntervalSeconds, status, spaceId, accessId, lastSeenAt, registeredAt);
    }

    public void assignToSpace(Long newSpaceId) {
        requireActive();
        if (type != DeviceType.SPACE_SENSOR) {
            throw new IllegalStateException("Device is not a space sensor.");
        }
        positive(newSpaceId, "spaceId");
        spaceId = newSpaceId;
        accessId = null;
        status = DeviceStatus.ASSIGNED;
    }

    public void assignToAccess(Long newAccessId) {
        requireActive();
        if (type != DeviceType.PASS_SENSOR) {
            throw new IllegalStateException("Device is not a passage sensor.");
        }
        positive(newAccessId, "accessId");
        accessId = newAccessId;
        spaceId = null;
        status = DeviceStatus.ASSIGNED;
    }

    public void decommission() {
        requireActive();
        status = DeviceStatus.DECOMMISSIONED;
        spaceId = null;
        accessId = null;
    }

    public boolean recordReport(Instant reportedAt) {
        requireActive();
        Objects.requireNonNull(reportedAt, "reportedAt is required.");
        if (lastSeenAt != null && !reportedAt.isAfter(lastSeenAt)) return false;
        lastSeenAt = reportedAt;
        return true;
    }

    public boolean isOverdue(Instant now) {
        Objects.requireNonNull(now, "now is required.");
        Instant reference = lastSeenAt == null ? registeredAt : lastSeenAt;
        return status != DeviceStatus.DECOMMISSIONED
                && now.isAfter(reference.plusSeconds(reportIntervalSeconds));
    }

    private void requireActive() {
        if (status == DeviceStatus.DECOMMISSIONED) {
            throw new IllegalStateException("Decommissioned device cannot be changed.");
        }
    }

    private static void positive(Long id, String name) {
        if (id == null || id <= 0) throw new IllegalArgumentException(name + " must be positive.");
    }

    public Long getId() { return id; }
    public Long getInstitutionId() { return institutionId; }
    public Long getParkingLotId() { return parkingLotId; }
    public String getHardwareId() { return hardwareId.value(); }
    public DeviceType getType() { return type; }
    public int getReportIntervalSeconds() { return reportIntervalSeconds; }
    public DeviceStatus getStatus() { return status; }
    public Long getSpaceId() { return spaceId; }
    public Long getAccessId() { return accessId; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getRegisteredAt() { return registeredAt; }
}

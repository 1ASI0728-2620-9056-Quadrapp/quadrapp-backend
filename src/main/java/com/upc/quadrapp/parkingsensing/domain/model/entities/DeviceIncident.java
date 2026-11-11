package com.upc.quadrapp.parkingsensing.domain.model.entities;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import java.time.Instant;
import java.util.Objects;

public final class DeviceIncident {
    private final Long id;
    private final Long institutionId;
    private final Long parkingLotId;
    private final Long deviceId;
    private final HardwareIdentifier hardwareId;
    private final IncidentType type;
    private final String details;
    private final Instant occurredAt;
    private Instant resolvedAt;

    private DeviceIncident(Long id, Long institutionId, Long parkingLotId, Long deviceId,
                           String hardwareId, IncidentType type, String details,
                           Instant occurredAt, Instant resolvedAt) {
        if (id != null) positive(id, "id");
        positive(institutionId, "institutionId");
        if (parkingLotId != null) positive(parkingLotId, "parkingLotId");
        if (deviceId != null) positive(deviceId, "deviceId");
        this.id = id;
        this.institutionId = institutionId;
        this.parkingLotId = parkingLotId;
        this.deviceId = deviceId;
        this.hardwareId = new HardwareIdentifier(hardwareId);
        this.type = Objects.requireNonNull(type, "type is required.");
        this.details = details == null ? "" : details.trim();
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required.");
        if (resolvedAt != null && resolvedAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException("resolvedAt must not precede occurredAt.");
        }
        this.resolvedAt = resolvedAt;
    }

    public static DeviceIncident open(Long institutionId, Long parkingLotId, Long deviceId,
                                      String hardwareId, IncidentType type, String details, Instant occurredAt) {
        return new DeviceIncident(null, institutionId, parkingLotId, deviceId,
                hardwareId, type, details, occurredAt, null);
    }

    public static DeviceIncident restore(Long id, Long institutionId, Long parkingLotId,
                                         Long deviceId, String hardwareId, IncidentType type,
                                         String details, Instant occurredAt, Instant resolvedAt) {
        positive(id, "id");
        return new DeviceIncident(id, institutionId, parkingLotId, deviceId,
                hardwareId, type, details, occurredAt, resolvedAt);
    }

    public void resolve(Instant at) {
        Objects.requireNonNull(at, "at is required.");
        if (resolvedAt != null) throw new IllegalStateException("Incident already resolved.");
        if (at.isBefore(occurredAt)) throw new IllegalArgumentException("Resolution precedes occurrence.");
        resolvedAt = at;
    }

    public boolean isOpen() { return resolvedAt == null; }
    public Long getId() { return id; }
    public Long getInstitutionId() { return institutionId; }
    public Long getParkingLotId() { return parkingLotId; }
    public Long getDeviceId() { return deviceId; }
    public String getHardwareId() { return hardwareId.value(); }
    public IncidentType getType() { return type; }
    public String getDetails() { return details; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    private static void positive(Long id, String name) {
        if (id == null || id <= 0) throw new IllegalArgumentException(name + " must be positive.");
    }
}

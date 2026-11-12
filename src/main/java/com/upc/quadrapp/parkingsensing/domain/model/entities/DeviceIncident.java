package com.upc.quadrapp.parkingsensing.domain.model.entities;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentCategory;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;

import java.time.Instant;
import java.util.Objects;

public final class DeviceIncident {

    private final Long id;
    private final Long institutionId;
    private final Long parkingLotId;
    private final Long deviceId;
    private final Long reportedBy;

    private final String hardwareId;
    private final String details;

    private final IncidentType type;
    private final IncidentCategory category;
    private final Instant occurredAt;

    private Long resolvedBy;
    private Instant resolvedAt;

    private DeviceIncident(
            Long id,
            Long institutionId,
            Long parkingLotId,
            Long deviceId,
            String hardwareId,
            IncidentType type,
            IncidentCategory category,
            Long reportedBy,
            Long resolvedBy,
            String details,
            Instant occurredAt,
            Instant resolvedAt
    ) {
        if (institutionId == null || institutionId <= 0 ||
                parkingLotId == null || parkingLotId <= 0 ||
                (id != null && id <= 0)) {
            throw new IllegalArgumentException(
                    "Incident requires a valid institution and parking lot."
            );
        }

        this.type = Objects.requireNonNull(
                type, "Incident type is required."
        );

        if ((type == IncidentType.USER_REPORT) !=
                (reportedBy != null && category != null)) {
            throw new IllegalArgumentException(
                    "USER_REPORT requires reportedBy and category; automated incidents must not set them."
            );
        }

        this.id = id;
        this.institutionId = institutionId;
        this.parkingLotId = parkingLotId;
        this.deviceId = deviceId;
        this.hardwareId = hardwareId;

        this.category = category;
        this.reportedBy = reportedBy;
        this.resolvedBy = resolvedBy;
        this.details = details;

        this.occurredAt = Objects.requireNonNull(
                occurredAt, "Detection time is required."
        );

        if (resolvedAt != null &&
                resolvedAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException(
                    "Resolution cannot precede detection."
            );
        }

        if ((resolvedAt == null) != (resolvedBy == null)) {
            throw new IllegalArgumentException(
                    "Resolution requires both actor and timestamp."
            );
        }

        this.resolvedAt = resolvedAt;
    }

    public static DeviceIncident open(
            Long institutionId,
            Long parkingLotId,
            Long deviceId,
            String hardwareId,
            IncidentType type,
            IncidentCategory category,
            Long reportedBy,
            String details,
            Instant occurredAt
    ) {
        return new DeviceIncident(
                null,
                institutionId,
                parkingLotId,
                deviceId,
                hardwareId,
                type,
                category,
                reportedBy,
                null,
                details,
                occurredAt,
                null
        );
    }

    public static DeviceIncident restore(
            Long id,
            Long institutionId,
            Long parkingLotId,
            Long deviceId,
            String hardwareId,
            IncidentType type,
            IncidentCategory category,
            Long reportedBy,
            Long resolvedBy,
            String details,
            Instant occurredAt,
            Instant resolvedAt
    ) {
        return new DeviceIncident(
                id,
                institutionId,
                parkingLotId,
                deviceId,
                hardwareId,
                type,
                category,
                reportedBy,
                resolvedBy,
                details,
                occurredAt,
                resolvedAt
        );
    }

    public void resolve(Long actorId, Instant resolvedAt) {
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException(
                    "Resolver is required."
            );
        }

        Objects.requireNonNull(
                resolvedAt, "Resolution time is required."
        );

        if (this.resolvedAt != null) {
            throw new IllegalStateException(
                    "Incident already resolved."
            );
        }

        if (resolvedAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException(
                    "Resolution cannot precede detection."
            );
        }

        this.resolvedBy = actorId;
        this.resolvedAt = resolvedAt;
    }

    public boolean isOpen() {
        return resolvedAt == null;
    }

    public Long getId() { return id; }
    public Long getInstitutionId() { return institutionId; }
    public Long getParkingLotId() { return parkingLotId; }
    public Long getDeviceId() { return deviceId; }

    public String getHardwareId() { return hardwareId; }
    public String getDetails() { return details; }

    public IncidentType getType() { return type; }
    public IncidentCategory getCategory() { return category; }

    public Long getReportedBy() { return reportedBy; }
    public Long getResolvedBy() { return resolvedBy; }

    public Instant getOccurredAt() { return occurredAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}

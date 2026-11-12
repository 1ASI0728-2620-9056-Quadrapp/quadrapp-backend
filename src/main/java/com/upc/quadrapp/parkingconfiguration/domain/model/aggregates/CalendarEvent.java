package com.upc.quadrapp.parkingconfiguration.domain.model.aggregates;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.CalendarEventType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventImpact;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

import java.time.Instant;
import java.util.Objects;

public final class CalendarEvent {

    private final Long id;
    private final Long institutionId;
    private final Long parkingLotId;
    private final Long zoneId;
    private final Long createdBy;

    private final String name;
    private final EventWindow window;
    private final CalendarEventType type;
    private final EventImpact impact;

    private final Instant cancelledAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CalendarEvent(
            Long id,
            Long institutionId,
            Long parkingLotId,
            Long zoneId,
            Long createdBy,
            String name,
            EventWindow window,
            CalendarEventType type,
            EventImpact impact,
            Instant cancelledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (institutionId == null ||
                institutionId <= 0 ||
                (id != null && id <= 0)) {
            throw new IllegalArgumentException(
                    "Invalid event ID."
            );
        }

        if (zoneId != null && parkingLotId == null) {
            throw new IllegalArgumentException(
                    "Zone-scoped event requires a parking lot."
            );
        }

        this.id = id;
        this.institutionId = institutionId;
        this.parkingLotId = parkingLotId;
        this.zoneId = zoneId;
        this.createdBy = createdBy;

        this.name = new ConfigurationName(name).value();

        if (this.name.length() > 120) {
            throw new IllegalArgumentException(
                    "Event name too long."
            );
        }

        this.window = Objects.requireNonNull(window);
        this.type = Objects.requireNonNull(
                type,
                "Event type is required."
        );
        this.impact = Objects.requireNonNull(
                impact,
                "Event impact is required."
        );

        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CalendarEvent schedule(
            Long institutionId,
            Long parkingLotId,
            Long zoneId,
            Long createdBy,
            String name,
            EventWindow window,
            CalendarEventType type,
            EventImpact impact
    ) {
        var now = Instant.now();

        return new CalendarEvent(
                null,
                institutionId,
                parkingLotId,
                zoneId,
                createdBy,
                name,
                window,
                type,
                impact == null ? EventImpact.MEDIUM : impact,
                null,
                now,
                now
        );
    }

    public static CalendarEvent restore(
            Long id,
            Long institutionId,
            Long parkingLotId,
            Long zoneId,
            Long createdBy,
            String name,
            EventWindow window,
            CalendarEventType type,
            EventImpact impact,
            Instant cancelledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new CalendarEvent(
                id,
                institutionId,
                parkingLotId,
                zoneId,
                createdBy,
                name,
                window,
                type,
                impact,
                cancelledAt,
                createdAt,
                updatedAt
        );
    }

    public Long getId() { return id; }

    public Long getInstitutionId() { return institutionId; }

    public Long getParkingLotId() { return parkingLotId; }

    public Long getZoneId() { return zoneId; }

    public Long getCreatedBy() { return createdBy; }

    public String getName() { return name; }

    public EventWindow getWindow() { return window; }

    public CalendarEventType getType() { return type; }

    public EventImpact getImpact() { return impact; }

    public Instant getCancelledAt() { return cancelledAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}

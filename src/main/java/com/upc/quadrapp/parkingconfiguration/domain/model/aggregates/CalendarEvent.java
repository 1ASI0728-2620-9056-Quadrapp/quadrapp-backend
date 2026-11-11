package com.upc.quadrapp.parkingconfiguration.domain.model.aggregates;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.ConfigurationName;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

public final class CalendarEvent {

    private final Long id;
    private final Long institutionId;
    private final Long parkingLotId;
    private final Long zoneId;
    private final Long createdBy;
    private final String name;
    private final EventWindow window;

    private CalendarEvent(
            Long id,
            Long institutionId,
            Long parkingLotId,
            Long zoneId,
            Long createdBy,
            String name,
            EventWindow window
    ) {
        if (id != null) {
            requirePositive(id, "id");
        }

        requirePositive(institutionId, "institutionId");

        if (parkingLotId != null) {
            requirePositive(parkingLotId, "parkingLotId");
        }

        if (zoneId != null) {
            requirePositive(zoneId, "zoneId");
        }

        if (createdBy != null) {
            requirePositive(createdBy, "createdBy");
        }

        if (zoneId != null && parkingLotId == null) {
            throw new IllegalArgumentException(
                    "A zone-scoped event requires a parking lot."
            );
        }

        if (window == null) {
            throw new IllegalArgumentException(
                    "Event window is required."
            );
        }

        this.id = id;
        this.institutionId = institutionId;
        this.parkingLotId = parkingLotId;
        this.zoneId = zoneId;
        this.createdBy = createdBy;
        this.name = new ConfigurationName(name).value();
        this.window = window;
    }

    public static CalendarEvent schedule(
            Long institutionId,
            Long parkingLotId,
            Long zoneId,
            Long createdBy,
            String name,
            EventWindow window
    ) {
        return new CalendarEvent(
                null,
                institutionId,
                parkingLotId,
                zoneId,
                createdBy,
                name,
                window
        );
    }

    public static CalendarEvent restore(
            Long id,
            Long institutionId,
            Long parkingLotId,
            Long zoneId,
            Long createdBy,
            String name,
            EventWindow window
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "id is required."
            );
        }

        return new CalendarEvent(
                id,
                institutionId,
                parkingLotId,
                zoneId,
                createdBy,
                name,
                window
        );
    }

    public Long getId() {
        return id;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public EventWindow getWindow() {
        return window;
    }

    private static void requirePositive(
            Long value,
            String field
    ) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    field + " must be positive."
            );
        }
    }
}

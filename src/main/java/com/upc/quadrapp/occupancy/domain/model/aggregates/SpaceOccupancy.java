package com.upc.quadrapp.occupancy.domain.model.aggregates;

import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedFree;
import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedOccupied;
import com.upc.quadrapp.occupancy.domain.model.events.SpaceMarkedUnknown;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.UnknownReason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Business state of one space: FREE, OCCUPIED or UNKNOWN, with the reason when UNKNOWN and the
 * last detection applied. A new space never starts free, an inactive space never changes state,
 * and a detection already applied or older than the last applied one changes nothing.
 */
public final class SpaceOccupancy {

    private final Long spaceId;
    private final Long tenantId;
    private final Long parkingLotId;
    private Long zoneId;
    private Long levelId;
    private boolean active;
    private SpaceStatus status;
    private UnknownReason unknownReason;
    private Instant statusSince;
    private Instant lastDetectedAt;
    private String lastEventId;
    private final Long version;
    private final List<Object> domainEvents = new ArrayList<>();

    private SpaceOccupancy(Long spaceId, Long tenantId, Long parkingLotId, Long zoneId, Long levelId, boolean active,
                           SpaceStatus status, UnknownReason unknownReason, Instant statusSince,
                           Instant lastDetectedAt, String lastEventId, Long version) {
        requirePositive(spaceId, "Space ID");
        requirePositive(tenantId, "Tenant ID");
        requirePositive(parkingLotId, "Parking lot ID");
        requirePositive(zoneId, "Zone ID");
        requirePositive(levelId, "Level ID");
        Objects.requireNonNull(status);
        Objects.requireNonNull(statusSince);
        if ((status == SpaceStatus.UNKNOWN) != (unknownReason != null)) {
            throw new IllegalArgumentException("Only an UNKNOWN space has an unknown reason.");
        }
        this.spaceId = spaceId;
        this.tenantId = tenantId;
        this.parkingLotId = parkingLotId;
        this.zoneId = zoneId;
        this.levelId = levelId;
        this.active = active;
        this.status = status;
        this.unknownReason = unknownReason;
        this.statusSince = statusSince;
        this.lastDetectedAt = lastDetectedAt;
        this.lastEventId = lastEventId;
        this.version = version;
    }

    /** A space Occupancy sees for the first time in the layout: UNKNOWN until a detection arrives. */
    public static SpaceOccupancy initialize(Long spaceId, Long tenantId, Long parkingLotId, Long zoneId, Long levelId,
                                            boolean active, Instant now) {
        SpaceOccupancy space = new SpaceOccupancy(spaceId, tenantId, parkingLotId, zoneId, levelId, active,
                SpaceStatus.UNKNOWN, UnknownReason.NOT_YET_REPORTED, now, null, null, null);
        if (active) {
            space.registerUnknown(now);
        }
        return space;
    }

    public static SpaceOccupancy restore(Long spaceId, Long tenantId, Long parkingLotId, Long zoneId, Long levelId,
                                         boolean active, SpaceStatus status, UnknownReason unknownReason,
                                         Instant statusSince, Instant lastDetectedAt, String lastEventId,
                                         Long version) {
        return new SpaceOccupancy(spaceId, tenantId, parkingLotId, zoneId, levelId, active, status, unknownReason,
                statusSince, lastDetectedAt, lastEventId, version);
    }

    /**
     * Applies a confirmed detection. Returns false when it is ignored: the space is inactive, the
     * event was already applied, or it is not after the last applied detection.
     */
    public boolean applyDetection(String eventId, SpaceStatus presence, Instant detectedAt) {
        Objects.requireNonNull(presence);
        Objects.requireNonNull(detectedAt);
        if (presence == SpaceStatus.UNKNOWN) {
            throw new IllegalArgumentException("A detection cannot set a space to UNKNOWN.");
        }
        if (!active || Objects.equals(eventId, lastEventId)
                || (lastDetectedAt != null && !detectedAt.isAfter(lastDetectedAt))) {
            return false;
        }
        boolean changed = status != presence;
        lastDetectedAt = detectedAt;
        lastEventId = eventId;
        if (changed) {
            status = presence;
            unknownReason = null;
            statusSince = detectedAt;
            domainEvents.add(presence == SpaceStatus.OCCUPIED
                    ? new SpaceMarkedOccupied(spaceId, parkingLotId, zoneId, detectedAt)
                    : new SpaceMarkedFree(spaceId, parkingLotId, zoneId, detectedAt));
        }
        return true;
    }

    /** Turns the space UNKNOWN with the given reason; nothing changes if it already is. */
    public void markUnknown(UnknownReason reason, Instant at) {
        Objects.requireNonNull(reason);
        Objects.requireNonNull(at);
        if (status == SpaceStatus.UNKNOWN && unknownReason == reason) {
            return;
        }
        status = SpaceStatus.UNKNOWN;
        unknownReason = reason;
        statusSince = at;
        registerUnknown(at);
    }

    /**
     * Applies the placement and the active flag of the current layout. A space that comes back
     * into use has no trusted state, so it becomes UNKNOWN until its next detection.
     */
    public void applyLayout(Long zoneId, Long levelId, boolean active, Instant at) {
        requirePositive(zoneId, "Zone ID");
        requirePositive(levelId, "Level ID");
        this.zoneId = zoneId;
        this.levelId = levelId;
        boolean reactivated = active && !this.active;
        this.active = active;
        if (reactivated) {
            lastDetectedAt = null;
            lastEventId = null;
            markUnknown(UnknownReason.NOT_YET_REPORTED, at);
        }
    }

    public boolean countsAsFree() {
        return active && status == SpaceStatus.FREE;
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void registerUnknown(Instant at) {
        domainEvents.add(new SpaceMarkedUnknown(tenantId, spaceId, parkingLotId, zoneId,
                SpaceMarkedUnknown.Reason.valueOf(unknownReason.name()), at));
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getLevelId() {
        return levelId;
    }

    public boolean isActive() {
        return active;
    }

    public SpaceStatus getStatus() {
        return status;
    }

    public UnknownReason getUnknownReason() {
        return unknownReason;
    }

    public Instant getStatusSince() {
        return statusSince;
    }

    public Instant getLastDetectedAt() {
        return lastDetectedAt;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public Long getVersion() {
        return version;
    }
}

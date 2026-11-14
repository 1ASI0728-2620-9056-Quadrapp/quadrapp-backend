package com.upc.quadrapp.occupancy.domain.model.aggregates;

import com.upc.quadrapp.occupancy.domain.model.events.LowAvailabilityEnded;
import com.upc.quadrapp.occupancy.domain.model.events.LowAvailabilityStarted;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.ZoneCount;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Availability of a parking lot, recalculated from the stored space states, and its
 * low-availability condition. The condition starts when the free share of the known spaces
 * (free over free plus occupied) falls below the lot's threshold and ends only when it rises above
 * that threshold plus {@value #HYSTERESIS_POINTS} points, so it does not flap with every space
 * that is freed and taken again. UNKNOWN spaces are left out: they are neither free nor taken.
 */
public final class LotOccupancy {

    public static final int HYSTERESIS_POINTS = 5;

    private final Long parkingLotId;
    private final Long tenantId;
    private OccupancyCounts counts;
    private int lowAvailabilityPct;
    private boolean lowAvailability;
    private Instant consolidatedAt;
    private Instant layoutSyncedAt;
    private final Long version;
    private final List<Object> domainEvents = new ArrayList<>();

    private LotOccupancy(Long parkingLotId, Long tenantId, OccupancyCounts counts, int lowAvailabilityPct,
                         boolean lowAvailability, Instant consolidatedAt, Instant layoutSyncedAt, Long version) {
        if (parkingLotId == null || parkingLotId <= 0 || tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Parking lot and tenant IDs must be positive.");
        }
        requireThreshold(lowAvailabilityPct);
        this.parkingLotId = parkingLotId;
        this.tenantId = tenantId;
        this.counts = Objects.requireNonNull(counts);
        this.lowAvailabilityPct = lowAvailabilityPct;
        this.lowAvailability = lowAvailability;
        this.consolidatedAt = consolidatedAt;
        this.layoutSyncedAt = Objects.requireNonNull(layoutSyncedAt);
        this.version = version;
    }

    public static LotOccupancy restore(Long parkingLotId, Long tenantId, OccupancyCounts counts,
                                       int lowAvailabilityPct, boolean lowAvailability, Instant consolidatedAt,
                                       Instant layoutSyncedAt, Long version) {
        return new LotOccupancy(parkingLotId, tenantId, counts, lowAvailabilityPct, lowAvailability,
                consolidatedAt, layoutSyncedAt, version);
    }

    /** Keeps the threshold of the current layout and the moment it was read. */
    public void applyLayout(int lowAvailabilityPct, Instant at) {
        requireThreshold(lowAvailabilityPct);
        this.lowAvailabilityPct = lowAvailabilityPct;
        this.layoutSyncedAt = Objects.requireNonNull(at);
    }

    /**
     * Replaces the counts with the sum of the zones and registers {@link LowAvailabilityStarted}
     * or {@link LowAvailabilityEnded} when the free share of the known spaces crosses the
     * threshold. Without any known space (all UNKNOWN) there is no information to decide, so the
     * condition stays as it was.
     */
    public void recalculate(List<ZoneCount> zones, Instant at) {
        Objects.requireNonNull(at);
        counts = sum(zones);
        consolidatedAt = at;
        long capacity = counts.totalSpaces();
        long free = counts.freeCount();
        long known = free + counts.occupiedCount();
        if (known == 0) {
            return;
        }
        if (!lowAvailability && free * 100 < (long) lowAvailabilityPct * known) {
            lowAvailability = true;
            domainEvents.add(new LowAvailabilityStarted(tenantId, parkingLotId, free, capacity, at));
        } else if (lowAvailability && free * 100 > (long) (lowAvailabilityPct + HYSTERESIS_POINTS) * known) {
            lowAvailability = false;
            domainEvents.add(new LowAvailabilityEnded(tenantId, parkingLotId, free, capacity, at));
        }
    }

    public static OccupancyCounts sum(List<ZoneCount> zones) {
        long total = 0;
        long free = 0;
        long occupied = 0;
        long unknown = 0;
        for (ZoneCount zone : zones) {
            total += zone.counts().totalSpaces();
            free += zone.counts().freeCount();
            occupied += zone.counts().occupiedCount();
            unknown += zone.counts().unknownCount();
        }
        return new OccupancyCounts(total, free, occupied, unknown);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private static void requireThreshold(int lowAvailabilityPct) {
        if (lowAvailabilityPct < 5 || lowAvailabilityPct > 30) {
            throw new IllegalArgumentException("Low availability threshold must be between 5 and 30.");
        }
    }

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public OccupancyCounts getCounts() {
        return counts;
    }

    public int getLowAvailabilityPct() {
        return lowAvailabilityPct;
    }

    public boolean isLowAvailability() {
        return lowAvailability;
    }

    public Instant getConsolidatedAt() {
        return consolidatedAt;
    }

    public Instant getLayoutSyncedAt() {
        return layoutSyncedAt;
    }

    public Long getVersion() {
        return version;
    }
}

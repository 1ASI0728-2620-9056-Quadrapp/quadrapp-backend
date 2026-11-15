package com.upc.quadrapp.occupancy.application.internal.commandservices;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort.ParkingLayout;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort.SpacePlacement;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.events.OccupancyEventPublisherPort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.LotOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SpaceOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.LotOccupancy;
import com.upc.quadrapp.occupancy.domain.model.aggregates.SpaceOccupancy;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.ZoneCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps Occupancy's copy of each parking lot layout and recalculates its availability.
 * <p>
 * The layout comes only from the Parking Configuration facade. A parking lot is copied the first
 * time Occupancy sees it, and copied again when the copy is older than
 * {@code quadrapp.occupancy.layout-max-age} or when a detection names a space or access that the
 * copy does not have yet. Every write locks the parking lot row first, so detections of one lot
 * are applied one at a time and two replicas never copy the same layout at once.
 * Callers run inside a transaction.
 */
@Component
public class OccupancyStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OccupancyStateManager.class);

    /** A detection for a space the copy does not know refreshes it at most this often. */
    private static final Duration MISSING_TARGET_REFRESH = Duration.ofSeconds(10);

    private final LotOccupancyPersistencePort lots;
    private final SpaceOccupancyPersistencePort spaces;
    private final ParkingLayoutPort layouts;
    private final OccupancyEventPublisherPort events;
    private final Clock clock;
    private final Duration layoutMaxAge;

    public OccupancyStateManager(
            LotOccupancyPersistencePort lots,
            SpaceOccupancyPersistencePort spaces,
            ParkingLayoutPort layouts,
            OccupancyEventPublisherPort events,
            @Qualifier("parkingSensingClock") Clock clock,
            @Value("${quadrapp.occupancy.layout-max-age:PT1M}") Duration layoutMaxAge
    ) {
        this.lots = lots;
        this.spaces = spaces;
        this.layouts = layouts;
        this.events = events;
        this.clock = clock;
        this.layoutMaxAge = layoutMaxAge;
    }

    /**
     * The locked parking lot, copied from the configuration if Occupancy does not know it yet or
     * its copy is too old. Empty if the parking lot does not exist in the configuration.
     */
    public Optional<LotOccupancy> lockFresh(Long parkingLotId) {
        Instant now = clock.instant();
        Optional<LotOccupancy> lot = lots.findByIdForUpdate(parkingLotId);
        if (lot.isEmpty()) {
            Optional<ParkingLayout> layout = layouts.layoutOf(parkingLotId);
            if (layout.isEmpty()) {
                return Optional.empty();
            }
            lots.createIfAbsent(parkingLotId, layout.get().tenantId(), layout.get().lowAvailabilityPct(), now);
            LotOccupancy created = lots.findByIdForUpdate(parkingLotId).orElseThrow();
            return Optional.of(synchronize(created, layout.get(), now));
        }
        if (isOlderThan(lot.get(), layoutMaxAge, now)) {
            return Optional.of(refresh(lot.get(), now));
        }
        return lot;
    }

    /**
     * The parking lot with a layout no older than the maximum age, for reads. It locks the row
     * only when the copy has to be created or refreshed.
     */
    public Optional<LotOccupancy> fresh(Long parkingLotId) {
        Optional<LotOccupancy> lot = lots.findById(parkingLotId);
        if (lot.isPresent() && !isOlderThan(lot.get(), layoutMaxAge, clock.instant())) {
            return lot;
        }
        return lockFresh(parkingLotId);
    }

    /** Copies the layout again when a detection names something the copy lacks (rate-limited). */
    public Optional<LotOccupancy> refreshForMissingTarget(LotOccupancy lot) {
        Instant now = clock.instant();
        if (!isOlderThan(lot, MISSING_TARGET_REFRESH, now)) {
            return Optional.empty();
        }
        return Optional.of(refresh(lot, now));
    }

    /** Refreshes the copies older than the maximum age; used by the minute snapshot job. */
    public int refreshStaleLayouts(List<Long> parkingLotIds) {
        int refreshed = 0;
        for (Long parkingLotId : parkingLotIds) {
            Optional<LotOccupancy> lot = lots.findById(parkingLotId);
            if (lot.isPresent() && isOlderThan(lot.get(), layoutMaxAge, clock.instant())) {
                lockFresh(parkingLotId);
                refreshed++;
            }
        }
        return refreshed;
    }

    /**
     * Recalculates the availability of the locked lot from its stored spaces and saves it. The
     * result holds the saved lot (with its new version), which callers must use from then on.
     */
    public Recalculation recalculate(LotOccupancy lot, Instant at) {
        List<ZoneCount> zones = spaces.countByZone(lot.getParkingLotId());
        lot.recalculate(zones, at);
        List<Object> lotEvents = lot.pullDomainEvents();
        LotOccupancy saved = lots.save(lot);
        events.publish(lotEvents);
        return new Recalculation(saved, zones);
    }

    public record Recalculation(LotOccupancy lot, List<ZoneCount> zones) {}

    private LotOccupancy refresh(LotOccupancy lot, Instant now) {
        Optional<ParkingLayout> layout = layouts.layoutOf(lot.getParkingLotId());
        if (layout.isEmpty()) {
            LOGGER.warn("Parking lot {} no longer exists in the configuration; its occupancy is kept as it is.",
                    lot.getParkingLotId());
            return lot;
        }
        return synchronize(lot, layout.get(), now);
    }

    private LotOccupancy synchronize(LotOccupancy lot, ParkingLayout layout, Instant now) {
        Map<Long, SpaceOccupancy> known = spaces.findByParkingLotId(lot.getParkingLotId()).stream()
                .collect(Collectors.toMap(SpaceOccupancy::getSpaceId, Function.identity()));
        List<SpaceOccupancy> changed = new ArrayList<>();
        Set<Long> placed = new HashSet<>();
        for (SpacePlacement placement : layout.spaces()) {
            placed.add(placement.spaceId());
            SpaceOccupancy space = known.get(placement.spaceId());
            if (space == null) {
                changed.add(SpaceOccupancy.initialize(placement.spaceId(), lot.getTenantId(), lot.getParkingLotId(),
                        placement.zoneId(), placement.levelId(), placement.active(), now));
            } else if (!placement.zoneId().equals(space.getZoneId())
                    || !placement.levelId().equals(space.getLevelId())
                    || placement.active() != space.isActive()) {
                space.applyLayout(placement.zoneId(), placement.levelId(), placement.active(), now);
                changed.add(space);
            }
        }
        for (SpaceOccupancy space : known.values()) {
            if (!placed.contains(space.getSpaceId()) && space.isActive()) {
                space.applyLayout(space.getZoneId(), space.getLevelId(), false, now);
                changed.add(space);
            }
        }
        List<Object> spaceEvents = changed.stream().flatMap(space -> space.pullDomainEvents().stream()).toList();
        spaces.saveAll(changed);
        events.publish(spaceEvents);
        lots.replaceAccessPoints(lot.getParkingLotId(), layout.accessPointIds(), layout.flowCountingAccessPointIds());
        lot.applyLayout(layout.lowAvailabilityPct(), now);
        return recalculate(lot, now).lot();
    }

    private static boolean isOlderThan(LotOccupancy lot, Duration age, Instant now) {
        return lot.getLayoutSyncedAt().plus(age).isBefore(now);
    }
}

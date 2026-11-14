package com.upc.quadrapp.occupancy.application.internal.queryservices;

import com.upc.quadrapp.occupancy.application.internal.commandservices.OccupancyStateManager;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancySnapshotPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SpaceOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.LotOccupancy;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.exceptions.OccupancyNotFoundException;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyScopeType;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;
import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reads Occupancy's own state. Every resource of another institution answers exactly like a
 * missing one ("not found"), so its ID cannot be discovered. Reads refresh the copy of the layout
 * when it is older than its maximum age, so they run in a read-write transaction.
 */
@Service
public class OccupancyQueryServiceImpl implements OccupancyQueryService {

    /** History returned when no range is given; the full history is up to 8 weeks per minute. */
    static final Duration DEFAULT_HISTORY = Duration.ofHours(24);

    private final ParkingLayoutPort layouts;
    private final OccupancyStateManager state;
    private final SpaceOccupancyPersistencePort spaces;
    private final OccupancySnapshotPersistencePort snapshots;
    private final Clock clock;

    public OccupancyQueryServiceImpl(
            ParkingLayoutPort layouts,
            OccupancyStateManager state,
            SpaceOccupancyPersistencePort spaces,
            OccupancySnapshotPersistencePort snapshots,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.layouts = layouts;
        this.state = state;
        this.spaces = spaces;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OccupancySummary getByParkingLot(Long parkingLotId) {
        Long readable = layouts.readableParkingLot(parkingLotId)
                .orElseThrow(() -> new OccupancyNotFoundException("Parking lot not found."));
        return lotSummary(readable);
    }

    @Override
    @Transactional
    public OccupancySummary getByZone(Long zoneId) {
        Long parkingLotId = layouts.readableParkingLotOfZone(zoneId)
                .orElseThrow(() -> new OccupancyNotFoundException("Zone not found."));
        state.fresh(parkingLotId);
        return summary(OccupancyScopeType.ZONE, zoneId, spaces.countByZoneId(zoneId));
    }

    @Override
    @Transactional
    public OccupancySummary getByLevel(Long levelId) {
        Long parkingLotId = layouts.readableParkingLotOfLevel(levelId)
                .orElseThrow(() -> new OccupancyNotFoundException("Level not found."));
        state.fresh(parkingLotId);
        return summary(OccupancyScopeType.LEVEL, levelId, spaces.countByLevelId(levelId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OccupancySnapshot> getSnapshots(Long zoneId, Instant from, Instant to) {
        layouts.readableParkingLotOfZone(zoneId)
                .orElseThrow(() -> new OccupancyNotFoundException("Zone not found."));
        validateRange(from, to);
        Instant end = to == null ? clock.instant() : to;
        Instant start = from == null ? end.minus(DEFAULT_HISTORY) : from;
        return snapshots.findByZoneIdAndTakenAtBetween(zoneId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OccupancySnapshot> getSnapshotsByParkingLot(
            Long parkingLotId,
            Instant from,
            Instant to
    ) {
        layouts.readableParkingLot(parkingLotId)
                .orElseThrow(() -> new OccupancyNotFoundException("Parking lot not found."));
        validateRange(from, to);
        Instant end = to == null ? clock.instant() : to;
        Instant start = from == null ? end.minus(DEFAULT_HISTORY) : from;
        return snapshots.findByParkingLotIdAndTakenAtBetween(parkingLotId, start, end);
    }

    @Override
    @Transactional
    public OccupancySummary getByParkingLotForInternalUse(Long parkingLotId) {
        return lotSummary(parkingLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReadableParkingLot(Long parkingLotId) {
        return layouts.readableParkingLot(parkingLotId).isPresent();
    }

    private OccupancySummary lotSummary(Long parkingLotId) {
        OccupancyCounts counts = state.fresh(parkingLotId)
                .map(LotOccupancy::getCounts)
                .orElse(new OccupancyCounts(0, 0, 0, 0));
        return summary(OccupancyScopeType.PARKING_LOT, parkingLotId, counts);
    }

    private OccupancySummary summary(OccupancyScopeType scopeType, Long scopeId, OccupancyCounts counts) {
        return new OccupancySummary(scopeType, scopeId, clock.instant(), counts);
    }

    private static void validateRange(Instant from, Instant to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException(
                    "Both from and to timestamps must be provided together."
            );
        }
        if (from != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "The from timestamp must not be after to."
            );
        }
    }
}

package com.upc.quadrapp.occupancy.application.internal.queryservices;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancyHistoryPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyScopeType;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal reads of the snapshot history for other modules (Prediction). They do not check the
 * caller, so they are only used by system jobs, never by a REST endpoint.
 */
@Service
public class OccupancyHistoryQueryService {

    private static final Duration MAX_TOLERANCE = Duration.ofHours(1);
    private static final int MAX_WEEKS = 8;

    private final OccupancyHistoryPersistencePort history;

    public OccupancyHistoryQueryService(OccupancyHistoryPersistencePort history) {
        this.history = history;
    }

    public Optional<OccupancySummary> lotOccupancyAt(Long parkingLotId, Instant at, Duration tolerance) {
        Objects.requireNonNull(parkingLotId, "Parking lot ID is required.");
        Objects.requireNonNull(at, "Moment is required.");
        if (tolerance == null || tolerance.isNegative() || tolerance.compareTo(MAX_TOLERANCE) > 0) {
            throw new IllegalArgumentException("Tolerance must be between 0 and 1 hour.");
        }
        return history.findLotSnapshotClosestTo(parkingLotId, at, tolerance)
                .map(snapshot -> new OccupancySummary(
                        OccupancyScopeType.PARKING_LOT,
                        parkingLotId,
                        snapshot.takenAt(),
                        snapshot.counts()
                ));
    }

    public OccupancyHistoryPersistencePort.WeeklySlotAverage weeklySlotAverage(
            Long parkingLotId,
            Instant slotStart,
            Duration slotLength,
            int weeks
    ) {
        Objects.requireNonNull(parkingLotId, "Parking lot ID is required.");
        Objects.requireNonNull(slotStart, "Slot start is required.");
        if (slotLength == null || slotLength.isZero() || slotLength.isNegative()
                || slotLength.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("Slot length must be between 1 second and 1 hour.");
        }
        if (weeks < 1 || weeks > MAX_WEEKS) {
            throw new IllegalArgumentException("Weeks must be between 1 and " + MAX_WEEKS + ".");
        }
        return history.weeklySlotAverage(parkingLotId, slotStart, slotLength, weeks);
    }
}

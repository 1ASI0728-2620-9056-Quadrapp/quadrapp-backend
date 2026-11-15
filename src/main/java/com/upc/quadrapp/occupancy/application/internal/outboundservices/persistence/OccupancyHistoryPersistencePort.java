package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Bounded reads of the per-minute snapshot history of a parking lot. */
public interface OccupancyHistoryPersistencePort {

    /** Lot totals (all zones) of the snapshot closest to {@code at} within {@code ±tolerance}. */
    Optional<LotSnapshot> findLotSnapshotClosestTo(Long parkingLotId, Instant at, Duration tolerance);

    /**
     * Average occupancy of the minutes in {@code [slotStart - k weeks, + slotLength)} for k = 1..weeks.
     * Minutes where no space is known are skipped.
     */
    WeeklySlotAverage weeklySlotAverage(Long parkingLotId, Instant slotStart, Duration slotLength, int weeks);

    record LotSnapshot(Instant takenAt, OccupancyCounts counts) {}

    /** {@code averageOccupancyPct} is null when no week has data. */
    record WeeklySlotAverage(int weeksWithData, BigDecimal averageOccupancyPct) {}
}

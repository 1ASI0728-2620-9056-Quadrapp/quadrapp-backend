package com.upc.quadrapp.occupancy.interfaces.acl;

import com.upc.quadrapp.occupancy.application.internal.queryservices.OccupancyHistoryQueryService;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateParkingLotSnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class OccupancyContextFacade {

    private final OccupancyCommandService commands;
    private final OccupancyQueryService queries;
    private final OccupancyHistoryQueryService history;

    public OccupancyContextFacade(
            OccupancyCommandService commands,
            OccupancyQueryService queries,
            OccupancyHistoryQueryService history
    ) {
        this.commands = commands;
        this.queries = queries;
        this.history = history;
    }

    public OccupancyView occupancyByParkingLot(Long parkingLotId) {
        return OccupancyView.from(queries.getByParkingLot(parkingLotId));
    }

    public OccupancyView occupancyByZone(Long zoneId) {
        return OccupancyView.from(queries.getByZone(zoneId));
    }

    public OccupancyView occupancyByLevel(Long levelId) {
        return OccupancyView.from(queries.getByLevel(levelId));
    }

    public List<OccupancySnapshotView> snapshotsByZone(
            Long zoneId,
            Instant from,
            Instant to
    ) {
        return queries.getSnapshots(zoneId, from, to)
                .stream()
                .map(OccupancySnapshotView::from)
                .toList();
    }

    public List<OccupancySnapshotView> captureSnapshotsByParkingLot(Long parkingLotId) {
        return commands.handle(new CreateParkingLotSnapshotCommand(parkingLotId))
                .stream()
                .map(OccupancySnapshotView::from)
                .toList();
    }

    public List<OccupancySnapshotView> snapshotsByParkingLot(
            Long parkingLotId,
            Instant from,
            Instant to
    ) {
        return queries.getSnapshotsByParkingLot(parkingLotId, from, to)
                .stream()
                .map(OccupancySnapshotView::from)
                .toList();
    }

    public OccupancyView currentOccupancyForInternalUse(Long parkingLotId) {
        return OccupancyView.from(queries.getByParkingLotForInternalUse(parkingLotId));
    }

    /**
     * Lot totals of the minute snapshot closest to {@code at} within {@code ±tolerance}; empty
     * when there is none. Internal read for system jobs (it does not check the caller).
     */
    public Optional<OccupancyView> lotOccupancyAt(Long parkingLotId, Instant at, Duration tolerance) {
        return history.lotOccupancyAt(parkingLotId, at, tolerance).map(OccupancyView::from);
    }

    /**
     * Average occupancy (occupied / known spaces, in %) of the slot {@code [slotStart, +slotLength)}
     * on the same weekday of each of the past {@code weeks} weeks (1 to 8), with the number of
     * weeks that had data. Internal read for system jobs (it does not check the caller).
     */
    public WeeklySlotAverageView weeklySlotAverage(
            Long parkingLotId,
            Instant slotStart,
            Duration slotLength,
            int weeks
    ) {
        var average = history.weeklySlotAverage(parkingLotId, slotStart, slotLength, weeks);
        return new WeeklySlotAverageView(average.weeksWithData(), average.averageOccupancyPct());
    }

    /** {@code averageOccupancyPct} is null when no week has data. */
    public record WeeklySlotAverageView(int weeksWithData, BigDecimal averageOccupancyPct) {}

    public record OccupancyView(
            String scopeType,
            Long scopeId,
            Instant takenAt,
            long totalSpaces,
            long freeCount,
            long occupiedCount,
            long unknownCount
    ) {
        private static OccupancyView from(OccupancySummary summary) {
            return new OccupancyView(
                    summary.scopeType().name(),
                    summary.scopeId(),
                    summary.takenAt(),
                    summary.counts().totalSpaces(),
                    summary.counts().freeCount(),
                    summary.counts().occupiedCount(),
                    summary.counts().unknownCount()
            );
        }
    }

    public record OccupancySnapshotView(
            Long id,
            Long zoneId,
            Instant takenAt,
            long totalSpaces,
            long freeCount,
            long occupiedCount,
            long unknownCount
    ) {
        private static OccupancySnapshotView from(OccupancySnapshot snapshot) {
            return new OccupancySnapshotView(
                    snapshot.id(),
                    snapshot.zoneId(),
                    snapshot.takenAt(),
                    snapshot.counts().totalSpaces(),
                    snapshot.counts().freeCount(),
                    snapshot.counts().occupiedCount(),
                    snapshot.counts().unknownCount()
            );
        }
    }
}

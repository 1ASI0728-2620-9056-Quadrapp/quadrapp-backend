package com.upc.quadrapp.occupancy.interfaces.acl;

import com.upc.quadrapp.occupancy.domain.model.commands.CreateParkingLotSnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OccupancyContextFacade {

    private final OccupancyCommandService commands;
    private final OccupancyQueryService queries;

    public OccupancyContextFacade(
            OccupancyCommandService commands,
            OccupancyQueryService queries
    ) {
        this.commands = commands;
        this.queries = queries;
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

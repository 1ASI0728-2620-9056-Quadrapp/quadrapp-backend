package com.upc.quadrapp.occupancy.interfaces.acl;

import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
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

    public boolean recordSpaceDetection(
            Long institutionId,
            Long parkingLotId,
            Long spaceId,
            Long deviceId,
            String eventId,
            OccupancyEventType eventType,
            Instant occurredAt
    ) {
        return commands.handle(
                new RecordSensorEventCommand(
                        institutionId,
                        parkingLotId,
                        deviceId,
                        spaceId,
                        null,
                        eventId,
                        eventType,
                        occurredAt
                )
        );
    }

    public boolean recordPassageDetection(
            Long institutionId,
            Long parkingLotId,
            Long accessId,
            Long deviceId,
            String eventId,
            OccupancyEventType eventType,
            Instant occurredAt
    ) {
        return commands.handle(
                new RecordSensorEventCommand(
                        institutionId,
                        parkingLotId,
                        deviceId,
                        null,
                        accessId,
                        eventId,
                        eventType,
                        occurredAt
                )
        );
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

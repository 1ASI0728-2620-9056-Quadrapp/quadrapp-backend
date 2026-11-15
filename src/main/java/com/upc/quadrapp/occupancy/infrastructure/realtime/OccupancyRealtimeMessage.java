package com.upc.quadrapp.occupancy.infrastructure.realtime;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort.SpaceChange;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort.ZoneOccupancyChange;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Message sent to {@code /topic/parking-lots/{lotId}/occupancy}: the latest availability of one
 * zone and of its parking lot, and the spaces of that zone that changed since the last message.
 */
public record OccupancyRealtimeMessage(
        Long parkingLotId,
        Long zoneId,
        Counts zone,
        Counts parkingLot,
        boolean lowAvailability,
        Instant consolidatedAt,
        List<SpaceState> spaces
) {
    public record Counts(long totalSpaces, long freeCount, long occupiedCount, long unknownCount) {
        static Counts of(OccupancyCounts counts) {
            return new Counts(counts.totalSpaces(), counts.freeCount(), counts.occupiedCount(),
                    counts.unknownCount());
        }
    }

    public record SpaceState(Long spaceId, String status, String unknownReason, Instant statusSince) {
        static SpaceState of(SpaceChange change) {
            return new SpaceState(
                    change.spaceId(),
                    change.status().name(),
                    change.unknownReason() == null ? null : change.unknownReason().name(),
                    change.statusSince()
            );
        }
    }

    static OccupancyRealtimeMessage of(ZoneOccupancyChange change, Collection<SpaceChange> spaces) {
        return new OccupancyRealtimeMessage(
                change.parkingLotId(),
                change.zoneId(),
                Counts.of(change.zone()),
                Counts.of(change.parkingLot()),
                change.lowAvailability(),
                change.consolidatedAt(),
                spaces.stream().map(SpaceState::of).toList()
        );
    }

    static String destination(Long parkingLotId) {
        return "/topic/parking-lots/" + parkingLotId + "/occupancy";
    }
}

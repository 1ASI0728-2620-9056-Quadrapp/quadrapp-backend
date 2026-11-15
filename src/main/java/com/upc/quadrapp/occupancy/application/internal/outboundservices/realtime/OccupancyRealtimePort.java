package com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.UnknownReason;

import java.time.Instant;

/** Delivers occupancy changes to the subscribed clients (app and console). */
public interface OccupancyRealtimePort {

    void publish(ZoneOccupancyChange change);

    /**
     * Availability of a zone after one committed change. {@code lotVersion} is the version of the
     * parking lot saved by that change: it grows with every recalculation of the lot, so a higher
     * value is always a newer state, even when the changes arrive out of order.
     */
    record ZoneOccupancyChange(
            Long parkingLotId,
            Long zoneId,
            OccupancyCounts zone,
            OccupancyCounts parkingLot,
            boolean lowAvailability,
            Instant consolidatedAt,
            SpaceChange space,
            long lotVersion
    ) {}

    record SpaceChange(Long spaceId, SpaceStatus status, UnknownReason unknownReason, Instant statusSince) {}
}

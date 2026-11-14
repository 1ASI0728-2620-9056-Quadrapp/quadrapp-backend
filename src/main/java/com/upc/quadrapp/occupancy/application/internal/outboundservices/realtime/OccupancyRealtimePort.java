package com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.UnknownReason;

import java.time.Instant;

/** Delivers occupancy changes to the subscribed clients (app and console). */
public interface OccupancyRealtimePort {

    void publish(ZoneOccupancyChange change);

    record ZoneOccupancyChange(
            Long parkingLotId,
            Long zoneId,
            OccupancyCounts zone,
            OccupancyCounts parkingLot,
            boolean lowAvailability,
            Instant consolidatedAt,
            SpaceChange space
    ) {}

    record SpaceChange(Long spaceId, SpaceStatus status, UnknownReason unknownReason, Instant statusSince) {}
}

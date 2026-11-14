package com.upc.quadrapp.occupancy.application.internal.outboundservices.acl;

import java.util.List;
import java.util.Optional;

/** Port to Parking Configuration, the owner of the layout and of the parking lots. */
public interface ParkingLayoutPort {

    Optional<ParkingLayout> layoutOf(Long parkingLotId);

    /** The lot if the authenticated user can read it; another institution's lot looks missing. */
    Optional<Long> readableParkingLot(Long parkingLotId);

    Optional<Long> readableParkingLotOfZone(Long zoneId);

    Optional<Long> readableParkingLotOfLevel(Long levelId);

    record ParkingLayout(
            Long parkingLotId,
            Long tenantId,
            int lowAvailabilityPct,
            List<SpacePlacement> spaces,
            List<Long> accessPointIds,
            List<Long> flowCountingAccessPointIds
    ) {}

    record SpacePlacement(Long spaceId, Long zoneId, Long levelId, boolean active) {}
}


package com.upc.quadrapp.parkingconfiguration.interfaces.acl;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.*;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
@Component
@RequiredArgsConstructor
public class ParkingSensingConfigurationFacade {

    private final ParkingLotPersistencePort lots;
    private final ZonePersistencePort zones;
    private final LevelPersistencePort levels;
    private final SpacePersistencePort spaces;
    private final AccessPersistencePort accesses;

    public void requireParkingLot(
            Long institutionId,
            Long parkingLotId
    ) {
        var lot = lots.findById(parkingLotId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Parking lot not found."
                        )
                );

        if (!lot.getInstitutionId().equals(institutionId)) {
            throw new AccessDeniedException(
                    "Parking lot belongs to another institution."
            );
        }
    }

    public void requireSpace(
            Long institutionId,
            Long parkingLotId,
            Long spaceId
    ) {
        requireParkingLot(institutionId, parkingLotId);

        var space = spaces.findById(spaceId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Space not found."
                        )
                );

        var level = levels.findById(space.getLevelId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Space level not found."
                        )
                );

        var zone = zones.findById(level.getZoneId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Level zone not found."
                        )
                );

        if (!zone.getParkingLotId().equals(parkingLotId)) {
            throw new AccessDeniedException(
                    "Space does not belong to this parking lot."
            );
        }
    }

    @Transactional
    public void applyOccupancyDetection(
            Long institutionId,
            Long parkingLotId,
            Long spaceId,
            SpaceStatus status,
            Instant occurredAt,
            Instant receivedAt
    ) {
        requireParkingLot(institutionId, parkingLotId);

        var space = spaces.findByIdForOccupancyUpdate(spaceId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Space not found."
                        )
                );

        var level = levels.findById(space.getLevelId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Space level not found."
                        )
                );

        var zone = zones.findById(level.getZoneId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Level zone not found."
                        )
                );

        if (!zone.getParkingLotId().equals(parkingLotId)) {
            throw new AccessDeniedException(
                    "Space does not belong to this parking lot."
            );
        }

        var updatedSpace = space.applyOccupancyDetection(
                status,
                occurredAt,
                receivedAt
        );
        if (updatedSpace != space) {
            spaces.save(updatedSpace);
        }
    }

    public void requireAccess(
            Long institutionId,
            Long parkingLotId,
            Long accessId
    ) {
        requireParkingLot(institutionId, parkingLotId);

        var access = accesses.findById(accessId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Access not found."
                        )
                );

        if (!access.getParkingLotId().equals(parkingLotId)) {
            throw new AccessDeniedException(
                    "Access does not belong to this parking lot."
            );
        }
    }

    public int detectionSeconds(
            Long institutionId,
            Long parkingLotId,
            boolean occupied
    ) {
        requireParkingLot(institutionId, parkingLotId);

        var lot = lots.findById(parkingLotId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Parking lot not found."
                        )
                );

        return occupied
                ? lot.getOccupiedDebounceS()
                : lot.getFreedDebounceS();
    }
}

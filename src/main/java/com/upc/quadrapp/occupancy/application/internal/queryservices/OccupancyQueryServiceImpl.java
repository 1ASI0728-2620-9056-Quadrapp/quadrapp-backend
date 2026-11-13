package com.upc.quadrapp.occupancy.application.internal.queryservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancyReadPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancySnapshotPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.exceptions.OccupancyNotFoundException;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyScopeType;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancySummary;
import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OccupancyQueryServiceImpl implements OccupancyQueryService {

    private final OccupancyReadPersistencePort occupancyRead;
    private final OccupancySnapshotPersistencePort snapshots;
    private final ParkingConfigurationContextFacade parkingConfiguration;
    private final IamContextFacade iam;
    private final Clock clock;

    public OccupancyQueryServiceImpl(
            OccupancyReadPersistencePort occupancyRead,
            OccupancySnapshotPersistencePort snapshots,
            ParkingConfigurationContextFacade parkingConfiguration,
            IamContextFacade iam,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.occupancyRead = occupancyRead;
        this.snapshots = snapshots;
        this.parkingConfiguration = parkingConfiguration;
        this.iam = iam;
        this.clock = clock;
    }

    @Override
    public OccupancySummary getByParkingLot(Long parkingLotId) {
        requireAuthorizedParkingLot(parkingLotId);
        return summary(OccupancyScopeType.PARKING_LOT, parkingLotId);
    }

    @Override
    public OccupancySummary getByZone(Long zoneId) {
        Long parkingLotId = occupancyRead.findParkingLotIdForZone(zoneId)
                .orElseThrow(() -> new OccupancyNotFoundException("Zone not found."));
        requireAuthorizedParkingLot(parkingLotId);
        return summary(OccupancyScopeType.ZONE, zoneId);
    }

    @Override
    public OccupancySummary getByLevel(Long levelId) {
        Long parkingLotId = occupancyRead.findParkingLotIdForLevel(levelId)
                .orElseThrow(() -> new OccupancyNotFoundException("Level not found."));
        requireAuthorizedParkingLot(parkingLotId);
        return summary(OccupancyScopeType.LEVEL, levelId);
    }

    @Override
    public List<OccupancySnapshot> getSnapshots(Long zoneId, Instant from, Instant to) {
        Long parkingLotId = occupancyRead.findParkingLotIdForZone(zoneId)
                .orElseThrow(() -> new OccupancyNotFoundException("Zone not found."));
        requireAuthorizedParkingLot(parkingLotId);

        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException(
                    "Both from and to timestamps must be provided together."
            );
        }
        if (from != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "The from timestamp must not be after to."
            );
        }

        return from == null
                ? snapshots.findByZoneId(zoneId)
                : snapshots.findByZoneIdAndTakenAtBetween(zoneId, from, to);
    }

    private OccupancySummary summary(OccupancyScopeType scopeType, Long scopeId) {
        return new OccupancySummary(
                scopeType,
                scopeId,
                clock.instant(),
                occupancyRead.summarize(scopeType, scopeId)
        );
    }

    private void requireAuthorizedParkingLot(Long parkingLotId) {
        Long institutionId = iam.getCurrentUser().institutionId();
        if (parkingConfiguration.findParkingLot(institutionId, parkingLotId).isEmpty()) {
            throw new OccupancyNotFoundException("Parking lot not found.");
        }
    }
}

package com.upc.quadrapp.parkingconfiguration.application.internal.queryservices;

import com.upc.quadrapp.parkingconfiguration.domain.model.exceptions.ParkingConfigurationNotFoundException;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.*;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.*;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;

import java.util.List;
import java.util.Optional;

public final class ParkingConfigurationQueryServiceImpl
        implements ParkingConfigurationQueryService {

    private final ParkingLotPersistencePort lots;
    private final ZonePersistencePort zones;
    private final LevelPersistencePort levels;
    private final SpacePersistencePort spaces;
    private final AccessPersistencePort accesses;
    private final ParkingConfigurationAuthorizationPort authorization;

    public ParkingConfigurationQueryServiceImpl(
            ParkingLotPersistencePort lots,
            ZonePersistencePort zones,
            LevelPersistencePort levels,
            SpacePersistencePort spaces,
            AccessPersistencePort accesses,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        this.lots = lots;
        this.zones = zones;
        this.levels = levels;
        this.spaces = spaces;
        this.accesses = accesses;
        this.authorization = authorization;
    }

    @Override
    public Optional<ParkingLot> handle(
            GetParkingLotByIdQuery query
    ) {

        Optional<ParkingLot> lot = lots.findById(
                query.parkingLotId()
        );

        // A lot of another institution looks missing (404), so its existence is not revealed.
        return lot.filter(value ->
                authorization.canRead(value.getInstitutionId())
        );
    }

    @Override
    public List<ParkingLot> handle(
            GetParkingLotsByInstitutionQuery query
    ) {

        authorization.requireInstitutionRead(
                query.institutionId()
        );

        return lots.findByInstitutionId(
                query.institutionId()
        );
    }

    @Override
    public List<Zone> handle(
            GetZonesByParkingLotQuery query
    ) {

        ParkingLot lot = requireLot(
                query.parkingLotId(),
                "Parking lot not found."
        );

        return zones.findByParkingLotId(
                lot.getId()
        );
    }

    @Override
    public List<Level> handle(
            GetLevelsByZoneQuery query
    ) {

        Zone zone = requireZone(
                query.zoneId()
        );

        requireLot(
                zone.getParkingLotId(),
                "Zone not found."
        );

        return levels.findByZoneId(
                zone.getId()
        );
    }

    @Override
    public List<Space> handle(
            GetSpacesByLevelQuery query
    ) {

        Level level = levels.findById(
                query.levelId()
        ).orElseThrow(
                () -> new ParkingConfigurationNotFoundException(
                        "Level not found."
                )
        );

        Zone zone = requireZone(
                level.getZoneId()
        );

        requireLot(
                zone.getParkingLotId(),
                "Level not found."
        );

        return spaces.findByLevelId(
                level.getId()
        );
    }

    @Override
    public List<Access> handle(
            GetAccessesByParkingLotQuery query
    ) {

        ParkingLot lot = requireLot(
                query.parkingLotId(),
                "Parking lot not found."
        );

        return accesses.findByParkingLotId(
                lot.getId()
        );
    }

    /**
     * The parking lot that holds the requested resource. A lot of another institution answers
     * with the same "not found" message as a missing resource, so its existence is not revealed.
     */
    private ParkingLot requireLot(Long id, String notFoundMessage) {
        ParkingLot lot = lots.findById(id)
                .orElseThrow(
                        () -> new ParkingConfigurationNotFoundException(
                                notFoundMessage
                        )
                );

        if (!authorization.canRead(lot.getInstitutionId())) {
            throw new ParkingConfigurationNotFoundException(
                    notFoundMessage
            );
        }

        return lot;
    }

    private Zone requireZone(Long id) {

        return zones.findById(id)
                .orElseThrow(
                        () -> new ParkingConfigurationNotFoundException(
                                "Zone not found."
                        )
                );
    }
}

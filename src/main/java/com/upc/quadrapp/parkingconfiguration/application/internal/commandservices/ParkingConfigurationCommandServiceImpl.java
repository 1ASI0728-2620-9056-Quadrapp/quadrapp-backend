package com.upc.quadrapp.parkingconfiguration.application.internal.commandservices;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.*;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.*;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;

public final class ParkingConfigurationCommandServiceImpl
        implements ParkingConfigurationCommandService {

    private final ParkingLotPersistencePort lots;
    private final ZonePersistencePort zones;
    private final LevelPersistencePort levels;
    private final SpacePersistencePort spaces;
    private final AccessPersistencePort accesses;
    private final ParkingConfigurationAuthorizationPort authorization;

    public ParkingConfigurationCommandServiceImpl(
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
    public ParkingLot handle(RegisterParkingLotCommand command) {

        authorization.requireInstitutionWrite(
                command.institutionId()
        );

        ParkingLot value = ParkingLot.register(
                command.institutionId(),
                command.name()
        );

        if (lots.existsByInstitutionIdAndName(
                command.institutionId(),
                value.getName()
        )) {
            throw new IllegalStateException(
                    "Parking lot name already exists in this institution."
            );
        }

        return lots.save(value);
    }

    @Override
    public Zone handle(CreateZoneCommand command) {

        ParkingLot lot = requireLot(
                command.parkingLotId()
        );

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Zone value = Zone.create(
                lot.getId(),
                command.name()
        );

        if (zones.existsByParkingLotIdAndName(
                lot.getId(),
                value.getName()
        )) {
            throw new IllegalStateException(
                    "Zone name already exists in this parking lot."
            );
        }

        return zones.save(value);
    }

    @Override
    public Level handle(CreateLevelCommand command) {

        Zone zone = requireZone(command.zoneId());

        ParkingLot lot = requireLot(
                zone.getParkingLotId()
        );

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Level value = Level.create(
                zone.getId(),
                command.name()
        );

        if (levels.existsByZoneIdAndName(
                zone.getId(),
                value.getName()
        )) {
            throw new IllegalStateException(
                    "Level name already exists in this zone."
            );
        }

        return levels.save(value);
    }

    @Override
    public Space handle(CreateSpaceCommand command) {

        Level level = levels.findById(command.levelId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Level not found."
                        )
                );

        Zone zone = requireZone(
                level.getZoneId()
        );

        ParkingLot lot = requireLot(
                zone.getParkingLotId()
        );

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Space value = Space.create(
                level.getId(),
                command.label()
        );

        if (spaces.existsByLevelIdAndLabel(
                level.getId(),
                value.getLabel()
        )) {
            throw new IllegalStateException(
                    "Space label already exists in this level."
            );
        }

        return spaces.save(value);
    }

    @Override
    public Access handle(CreateAccessCommand command) {

        ParkingLot lot = requireLot(
                command.parkingLotId()
        );

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Access value = Access.create(
                lot.getId(),
                command.name()
        );

        if (accesses.existsByParkingLotIdAndName(
                lot.getId(),
                value.getName()
        )) {
            throw new IllegalStateException(
                    "Access name already exists in this parking lot."
            );
        }

        return accesses.save(value);
    }

    private ParkingLot requireLot(Long id) {

        return lots.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Parking lot not found."
                        )
                );
    }

    private Zone requireZone(Long id) {

        return zones.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Zone not found."
                        )
                );
    }
}

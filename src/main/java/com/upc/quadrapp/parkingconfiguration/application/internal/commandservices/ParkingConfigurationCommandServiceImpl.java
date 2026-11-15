package com.upc.quadrapp.parkingconfiguration.application.internal.commandservices;

import com.upc.quadrapp.parkingconfiguration.domain.model.exceptions.ParkingConfigurationNotFoundException;
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
                command.createdBy(),
                command.name(),
                command.address(),
                command.latitude(),
                command.longitude(),
                command.opensAt(),
                command.closesAt(),
                command.saturationThresholdPct() == null
                        ? 90 : command.saturationThresholdPct(),
                command.lowBatteryThresholdPct() == null
                        ? 15 : command.lowBatteryThresholdPct(),
                command.staleAfterSeconds() == null
                        ? 180 : command.staleAfterSeconds(),
                command.flowWindowMinutes() == null
                        ? 15 : command.flowWindowMinutes(),
                command.accuracyMarginPct() == null
                        ? 10 : command.accuracyMarginPct(),
                command.occupiedDebounceS() == null
                        ? 10 : command.occupiedDebounceS(),
                command.freedDebounceS() == null
                        ? 20 : command.freedDebounceS(),
                command.lowAvailabilityPct() == null
                        ? 10 : command.lowAvailabilityPct()
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

        ParkingLot lot = requireLot(command.parkingLotId(), "Parking lot not found.");

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Zone value = Zone.create(
                lot.getId(),
                command.name(),
                command.description(),
                command.sortOrder() == null
                        ? 0 : command.sortOrder()
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

        ParkingLot lot = requireLot(zone.getParkingLotId(), "Zone not found.");

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Level value = Level.create(
                zone.getId(),
                command.name(),
                command.sortOrder() == null
                        ? 0 : command.sortOrder()
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
                        () -> new ParkingConfigurationNotFoundException(
                                "Level not found."
                        )
                );

        Zone zone = requireZone(level.getZoneId());

        ParkingLot lot = requireLot(zone.getParkingLotId(), "Level not found.");

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Space value = Space.create(
                level.getId(),
                command.label(),
                command.type(),
                command.posX(),
                command.posY()
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

        ParkingLot lot = requireLot(command.parkingLotId(), "Parking lot not found.");

        authorization.requireInstitutionWrite(
                lot.getInstitutionId()
        );

        Access value = Access.create(
                lot.getId(),
                command.name(),
                command.type(),
                Boolean.TRUE.equals(command.flowCountingEnabled())
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

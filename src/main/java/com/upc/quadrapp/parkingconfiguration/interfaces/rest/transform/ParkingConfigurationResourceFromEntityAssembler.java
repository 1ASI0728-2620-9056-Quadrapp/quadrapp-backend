package com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;

import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;

public final class ParkingConfigurationResourceFromEntityAssembler {

    private ParkingConfigurationResourceFromEntityAssembler() {}

    public static ParkingLotResource toResourceFromEntity(ParkingLot value)
    {
        return new ParkingLotResource(
                value.getId(),
                value.getInstitutionId(),
                value.getCreatedBy(),
                value.getName(),
                value.getAddress(),
                value.getLatitude(),
                value.getLongitude(),
                value.getOpensAt(),
                value.getClosesAt(),
                value.getSaturationThresholdPct(),
                value.getLowBatteryThresholdPct(),
                value.getStaleAfterSeconds(),
                value.getFlowWindowMinutes(),
                value.getAccuracyMarginPct(),
                value.getOccupiedDebounceS(),
                value.getFreedDebounceS(),
                value.getLayoutVersion(),
                value.getLayoutPublishedAt(),
                value.isActive(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    public static ZoneResource toResourceFromEntity(Zone value) {
        return new ZoneResource(
                value.getId(),
                value.getParkingLotId(),
                value.getName(),
                value.getDescription(),
                value.getSortOrder(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    public static LevelResource toResourceFromEntity(Level value) {
        return new LevelResource(
                value.getId(),
                value.getZoneId(),
                value.getName(),
                value.getSortOrder(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    public static SpaceResource toResourceFromEntity(Space value) {
        return new SpaceResource(
                value.getId(),
                value.getLevelId(),
                value.getLabel(),
                value.getType(),
                value.getPosX(),
                value.getPosY(),
                value.getStatus(),
                value.getUnknownReason(),
                value.getStatusSince(),
                value.getLastEventAt(),
                value.isActive(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    public static AccessResource toResourceFromEntity(Access value) {
        return new AccessResource(
                value.getId(),
                value.getParkingLotId(),
                value.getName(),
                value.getType(),
                value.isFlowCountingEnabled(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }
}

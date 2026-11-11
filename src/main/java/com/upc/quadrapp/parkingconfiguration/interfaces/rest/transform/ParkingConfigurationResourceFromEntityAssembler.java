package com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;

public final class ParkingConfigurationResourceFromEntityAssembler {

    private ParkingConfigurationResourceFromEntityAssembler() {}

    public static ParkingLotResource toResourceFromEntity(
            ParkingLot value
    ) {
        return new ParkingLotResource(
                value.getId(),
                value.getInstitutionId(),
                value.getName()
        );
    }

    public static ZoneResource toResourceFromEntity(
            Zone value
    ) {
        return new ZoneResource(
                value.getId(),
                value.getParkingLotId(),
                value.getName()
        );
    }

    public static LevelResource toResourceFromEntity(
            Level value
    ) {
        return new LevelResource(
                value.getId(),
                value.getZoneId(),
                value.getName()
        );
    }

    public static SpaceResource toResourceFromEntity(
            Space value
    ) {
        return new SpaceResource(
                value.getId(),
                value.getLevelId(),
                value.getLabel()
        );
    }

    public static AccessResource toResourceFromEntity(
            Access value
    ) {
        return new AccessResource(
                value.getId(),
                value.getParkingLotId(),
                value.getName()
        );
    }
}

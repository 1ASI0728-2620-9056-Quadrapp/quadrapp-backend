package com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform;

import com.upc.quadrapp.parkingconfiguration.domain.model.commands.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;

public final class ParkingConfigurationCommandFromResourceAssembler {

    private ParkingConfigurationCommandFromResourceAssembler() {}

    public static RegisterParkingLotCommand toCommand(
            Long institutionId,
            RegisterParkingLotResource resource
    ) {
        return new RegisterParkingLotCommand(
                institutionId,
                resource.name()
        );
    }

    public static CreateZoneCommand toCommand(
            Long parkingLotId,
            CreateZoneResource resource
    ) {
        return new CreateZoneCommand(
                parkingLotId,
                resource.name()
        );
    }

    public static CreateLevelCommand toCommand(
            Long zoneId,
            CreateLevelResource resource
    ) {
        return new CreateLevelCommand(
                zoneId,
                resource.name()
        );
    }

    public static CreateSpaceCommand toCommand(
            Long levelId,
            CreateSpaceResource resource
    ) {
        return new CreateSpaceCommand(
                levelId,
                resource.label()
        );
    }

    public static CreateAccessCommand toCommand(
            Long parkingLotId,
            CreateAccessResource resource
    ) {
        return new CreateAccessCommand(
                parkingLotId,
                resource.name()
        );
    }
}

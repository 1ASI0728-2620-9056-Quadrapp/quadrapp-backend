package com.upc.quadrapp.parkingconfiguration.interfaces.rest.transform;

import com.upc.quadrapp.parkingconfiguration.domain.model.commands.*;
import com.upc.quadrapp.parkingconfiguration.interfaces.rest.resources.*;

public final class ParkingConfigurationCommandFromResourceAssembler {

    private ParkingConfigurationCommandFromResourceAssembler() {}

    public static RegisterParkingLotCommand toCommand(Long institutionId, Long actorId, RegisterParkingLotResource resource)
    {
        return new RegisterParkingLotCommand(
                institutionId,
                actorId,
                resource.name(),
                resource.address(),
                resource.latitude(),
                resource.longitude(),
                resource.opensAt(),
                resource.closesAt(),
                resource.saturationThresholdPct(),
                resource.lowBatteryThresholdPct(),
                resource.staleAfterSeconds(),
                resource.flowWindowMinutes(),
                resource.accuracyMarginPct(),
                resource.occupiedDebounceS(),
                resource.freedDebounceS()
        );
    }

    public static CreateZoneCommand toCommand(Long parkingLotId, CreateZoneResource resource)
    {
        return new CreateZoneCommand(
                parkingLotId,
                resource.name(),
                resource.description(),
                resource.sortOrder() == null
                        ? null
                        : resource.sortOrder().intValue()
        );
    }

    public static CreateLevelCommand toCommand(Long zoneId, CreateLevelResource resource)
    {
        return new CreateLevelCommand(
                zoneId,
                resource.name(),
                resource.sortOrder() == null
                        ? null
                        : resource.sortOrder().intValue()
        );
    }

    public static CreateSpaceCommand toCommand(Long levelId, CreateSpaceResource resource)
    {
        return new CreateSpaceCommand(
                levelId,
                resource.label(),
                resource.type(),
                resource.posX(),
                resource.posY()
        );
    }

    public static CreateAccessCommand toCommand(Long parkingLotId, CreateAccessResource resource)
    {
        return new CreateAccessCommand(
                parkingLotId,
                resource.name(),
                resource.type(),
                resource.flowCountingEnabled()
        );
    }
}

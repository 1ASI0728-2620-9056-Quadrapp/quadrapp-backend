package com.upc.quadrapp.parkingsensing.infrastructure.configuration.acl;

import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingSensingConfigurationFacade;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingConfigurationLookupAdapter implements ParkingConfigurationLookupPort {

    private final ParkingSensingConfigurationFacade configuration;

    @Override
    public void requireParkingLot(
            Long institutionId,
            Long parkingLotId
    ) {
        configuration.requireParkingLot(
                institutionId,
                parkingLotId
        );
    }

    @Override
    public void requireSpace(
            Long institutionId,
            Long parkingLotId,
            Long spaceId
    ) {
        configuration.requireSpace(
                institutionId,
                parkingLotId,
                spaceId
        );
    }

    @Override
    public void requireAccess(
            Long institutionId,
            Long parkingLotId,
            Long accessId
    ) {
        configuration.requireAccess(
                institutionId,
                parkingLotId,
                accessId
        );
    }

    @Override
    public int minimumDetectionSeconds(
            Long institutionId,
            Long parkingLotId,
            SensorState state
    ) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "Sensor state is required."
            );
        }

        return configuration.detectionSeconds(
                institutionId,
                parkingLotId,
                state == SensorState.OCCUPIED
        );
    }
}


package com.upc.quadrapp.parkingsensing.infrastructure.configuration.acl;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("dev")
public class StandaloneParkingConfigurationLookupAdapter
        implements ParkingConfigurationLookupPort {

    @Override
    public void requireParkingLot(
            Long institutionId,
            Long parkingLotId
    ) {
        throw unavailable();
    }

    @Override
    public void requireSpace(
            Long institutionId,
            Long parkingLotId,
            Long spaceId
    ) {
        throw unavailable();
    }

    @Override
    public void requireAccess(
            Long institutionId,
            Long parkingLotId,
            Long accessId
    ) {
        throw unavailable();
    }

    @Override
    public int minimumDetectionSeconds(
            Long institutionId,
            Long parkingLotId
    ) {
        throw unavailable();
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Parking Configuration is not integrated " +
                        "into the current development branch."
        );
    }
}

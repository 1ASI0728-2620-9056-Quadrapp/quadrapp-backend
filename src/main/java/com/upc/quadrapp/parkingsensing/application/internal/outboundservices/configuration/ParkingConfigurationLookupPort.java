package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;

public interface ParkingConfigurationLookupPort {
    void requireParkingLot(Long institutionId, Long parkingLotId);
    void requireSpace(Long institutionId, Long parkingLotId, Long spaceId);
    void requireAccess(Long institutionId, Long parkingLotId, Long accessId);
    int minimumDetectionSeconds(Long institutionId, Long parkingLotId, SensorState state);
}

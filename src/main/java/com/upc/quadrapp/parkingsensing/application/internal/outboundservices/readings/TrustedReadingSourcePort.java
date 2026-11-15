package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;

public interface TrustedReadingSourcePort {
    void requireTrustedSource(Long institutionId, SensorReading reading);
}
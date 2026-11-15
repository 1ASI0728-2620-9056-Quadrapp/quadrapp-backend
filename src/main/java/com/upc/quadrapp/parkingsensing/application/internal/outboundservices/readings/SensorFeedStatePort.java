package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;

public interface SensorFeedStatePort {
    SensorReadingResult accept(Long deviceId, int minimumDetectionSeconds, SensorReading reading);
    void reset(Long deviceId);
    void deactivate(Long deviceId);
}
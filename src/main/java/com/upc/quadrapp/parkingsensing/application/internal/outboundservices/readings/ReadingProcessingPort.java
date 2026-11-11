package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import java.util.function.Supplier;

public interface ReadingProcessingPort {
    SensorReadingResult processOnce(Long institutionId, String hardwareId,
                                    String eventId, Supplier<SensorReadingResult> operation);
}
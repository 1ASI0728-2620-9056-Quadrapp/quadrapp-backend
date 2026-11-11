package com.upc.quadrapp.parkingsensing.domain.model.commands;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import java.util.Objects;

public record ProcessSensorReadingCommand(Long institutionId, SensorReading reading) {
    public ProcessSensorReadingCommand {
        if (institutionId == null || institutionId <= 0) throw new IllegalArgumentException("institutionId must be positive.");
        Objects.requireNonNull(reading, "reading is required.");
    }
}

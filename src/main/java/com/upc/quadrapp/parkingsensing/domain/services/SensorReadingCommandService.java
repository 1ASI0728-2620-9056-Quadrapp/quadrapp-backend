package com.upc.quadrapp.parkingsensing.domain.services;

import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;

public interface SensorReadingCommandService {
    SensorReadingResult handle(ProcessSensorReadingCommand command);
}

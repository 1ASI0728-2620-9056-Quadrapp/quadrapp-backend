package com.upc.quadrapp.parkingconfiguration.domain.services;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.*;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;

public interface ParkingConfigurationCommandService {

    ParkingLot handle(RegisterParkingLotCommand command);

    Zone handle(CreateZoneCommand command);

    Level handle(CreateLevelCommand command);

    Space handle(CreateSpaceCommand command);

    Access handle(CreateAccessCommand command);
}

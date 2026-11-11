package com.upc.quadrapp.parkingsensing.domain.services;

import com.upc.quadrapp.parkingsensing.domain.model.commands.*;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;

public interface DeviceIncidentCommandService {
    DeviceIncident handle(ReportDeviceIncidentCommand command);
    DeviceIncident handle(ResolveDeviceIncidentCommand command);
}

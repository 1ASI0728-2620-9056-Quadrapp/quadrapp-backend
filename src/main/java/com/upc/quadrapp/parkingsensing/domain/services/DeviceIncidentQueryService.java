package com.upc.quadrapp.parkingsensing.domain.services;

import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceIncidentsQuery;
import java.util.List;

public interface DeviceIncidentQueryService {
    List<DeviceIncident> handle(GetDeviceIncidentsQuery query);
}

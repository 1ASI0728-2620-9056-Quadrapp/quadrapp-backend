package com.upc.quadrapp.parkingsensing.application.internal.queryservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.ParkingSensingAuthorizationPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DeviceIncidentPersistencePort;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceIncidentsQuery;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentQueryService;
import java.util.List;
import java.util.Objects;

public final class DeviceIncidentQueryServiceImpl implements DeviceIncidentQueryService {
    private final DeviceIncidentPersistencePort incidents;
    private final ParkingConfigurationLookupPort configuration;
    private final ParkingSensingAuthorizationPort authorization;

    public DeviceIncidentQueryServiceImpl(DeviceIncidentPersistencePort incidents,
                                          ParkingConfigurationLookupPort configuration,
                                          ParkingSensingAuthorizationPort authorization) {
        this.incidents = Objects.requireNonNull(incidents);
        this.configuration = Objects.requireNonNull(configuration);
        this.authorization = Objects.requireNonNull(authorization);
    }

    @Override
    public List<DeviceIncident> handle(GetDeviceIncidentsQuery query) {
        authorization.requireInstitutionRead(query.institutionId());
        if (query.parkingLotId() != null) {
            configuration.requireParkingLot(query.institutionId(), query.parkingLotId());
        }
        return incidents.findByInstitutionIdAndOptionalParkingLotId(
                query.institutionId(), query.parkingLotId(), query.onlyOpen());
    }
}
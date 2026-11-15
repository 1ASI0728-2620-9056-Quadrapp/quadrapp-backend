package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceIncidentsQuery;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import java.util.List;
import java.util.Optional;

public interface DeviceIncidentPersistencePort {
    DeviceIncident save(DeviceIncident incident);
    Optional<DeviceIncident> findById(Long incidentId);

    boolean existsOpen(Long deviceId, IncidentType type);
    /** A {@code null} parking lot or status does not filter. */
    List<DeviceIncident> findByInstitutionIdAndOptionalParkingLotId(
            Long institutionId, Long parkingLotId, GetDeviceIncidentsQuery.Status status);
}
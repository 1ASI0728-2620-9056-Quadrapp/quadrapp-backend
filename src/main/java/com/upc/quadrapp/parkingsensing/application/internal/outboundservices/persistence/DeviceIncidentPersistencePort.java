package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import java.util.List;
import java.util.Optional;

public interface DeviceIncidentPersistencePort {
    DeviceIncident save(DeviceIncident incident);
    Optional<DeviceIncident> findById(Long incidentId);
    List<DeviceIncident> findByInstitutionIdAndOptionalParkingLotId(
            Long institutionId, Long parkingLotId, boolean onlyOpen);
}
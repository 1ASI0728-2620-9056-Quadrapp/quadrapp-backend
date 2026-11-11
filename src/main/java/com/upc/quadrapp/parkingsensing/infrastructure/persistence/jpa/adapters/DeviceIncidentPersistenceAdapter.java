package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DeviceIncidentPersistencePort;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.mappers.ParkingSensingJpaMapper;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories.DeviceIncidentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceIncidentPersistenceAdapter implements DeviceIncidentPersistencePort {
    private final DeviceIncidentJpaRepository repository;
    private final ParkingSensingJpaMapper mapper;

    @Override
    public DeviceIncident save(DeviceIncident incident) {
        return mapper.toDomain(repository.save(mapper.toJpa(incident)));
    }

    @Override
    public Optional<DeviceIncident> findById(Long incidentId) {
        return repository.findById(incidentId).map(mapper::toDomain);
    }

    @Override
    public List<DeviceIncident> findByInstitutionIdAndOptionalParkingLotId(
            Long institutionId, Long parkingLotId, boolean onlyOpen) {
        var result = parkingLotId == null
                ? (onlyOpen
                   ? repository.findByInstitutionIdAndResolvedAtIsNullOrderByOccurredAtDesc(institutionId)
                   : repository.findByInstitutionIdOrderByOccurredAtDesc(institutionId))
                : (onlyOpen
                   ? repository.findByInstitutionIdAndParkingLotIdAndResolvedAtIsNullOrderByOccurredAtDesc(institutionId, parkingLotId)
                   : repository.findByInstitutionIdAndParkingLotIdOrderByOccurredAtDesc(institutionId, parkingLotId));
        return result.stream().map(mapper::toDomain).toList();
    }
}
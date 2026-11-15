package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DeviceIncidentPersistencePort;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceIncidentsQuery;

import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceIncidentJpaEntity;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.mappers.ParkingSensingJpaMapper;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories.DeviceIncidentJpaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceIncidentPersistenceAdapter implements DeviceIncidentPersistencePort {

    private final DeviceIncidentJpaRepository repository;
    private final ParkingSensingJpaMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public DeviceIncident save(DeviceIncident incident) {
        var saved = repository.saveAndFlush(
                mapper.toJpa(incident)
        );

        entityManager.refresh(saved);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<DeviceIncident> findById(Long incidentId) {
        return repository.findById(incidentId)
                .map(mapper::toDomain);
    }

    @Override
    public List<DeviceIncident> findByInstitutionIdAndOptionalParkingLotId(
            Long institutionId,
            Long parkingLotId,
            GetDeviceIncidentsQuery.Status status
    ) {
        List<DeviceIncidentJpaEntity> result;

        if (parkingLotId == null) {
            result = status == null
                    ? repository.findByInstitutionIdOrderByOccurredAtDesc(institutionId)
                    : switch (status) {
                        case OPEN -> repository.findByInstitutionIdAndResolvedAtIsNullOrderByOccurredAtDesc(
                                institutionId);
                        case RESOLVED -> repository.findByInstitutionIdAndResolvedAtIsNotNullOrderByOccurredAtDesc(
                                institutionId);
                    };
        } else {
            result = status == null
                    ? repository.findByInstitutionIdAndParkingLotIdOrderByOccurredAtDesc(institutionId, parkingLotId)
                    : switch (status) {
                        case OPEN -> repository
                                .findByInstitutionIdAndParkingLotIdAndResolvedAtIsNullOrderByOccurredAtDesc(
                                        institutionId, parkingLotId);
                        case RESOLVED -> repository
                                .findByInstitutionIdAndParkingLotIdAndResolvedAtIsNotNullOrderByOccurredAtDesc(
                                        institutionId, parkingLotId);
                    };
        }

        return result.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsOpen(Long deviceId, IncidentType type) {
        return repository.existsByDeviceIdAndTypeAndResolvedAtIsNull(deviceId, type);
    }
}

package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.ParkingLotPersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers.ParkingConfigurationJpaMapper;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories.ParkingLotJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ParkingLotPersistenceAdapter
        implements ParkingLotPersistencePort {

    private final ParkingLotJpaRepository repository;
    private final ParkingConfigurationJpaMapper mapper;

    @Override
    public ParkingLot save(ParkingLot value) {
        return mapper.toDomain(
                repository.save(mapper.toJpa(value))
        );
    }

    @Override
    public Optional<ParkingLot> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<ParkingLot> findByInstitutionId(
            Long institutionId
    ) {
        return repository.findByInstitutionId(institutionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByInstitutionIdAndName(
            Long institutionId,
            String name
    ) {
        return repository.existsByInstitutionIdAndName(
                institutionId,
                name
        );
    }
}

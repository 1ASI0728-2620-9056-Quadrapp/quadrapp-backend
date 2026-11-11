package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.AccessPersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Access;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers.ParkingConfigurationJpaMapper;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories.AccessJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccessPersistenceAdapter
        implements AccessPersistencePort {

    private final AccessJpaRepository repository;
    private final ParkingConfigurationJpaMapper mapper;

    @Override
    public Access save(Access value) {
        return mapper.toDomain(
                repository.save(mapper.toJpa(value))
        );
    }

    @Override
    public Optional<Access> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Access> findByParkingLotId(Long parkingLotId) {
        return repository.findByParkingLotId(parkingLotId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByParkingLotIdAndName(
            Long parkingLotId,
            String name
    ) {
        return repository.existsByParkingLotIdAndName(
                parkingLotId,
                name
        );
    }
}

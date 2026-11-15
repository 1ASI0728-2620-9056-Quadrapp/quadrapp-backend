package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.ZonePersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Zone;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers.ParkingConfigurationJpaMapper;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories.ZoneJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ZonePersistenceAdapter
        implements ZonePersistencePort {

    private final ZoneJpaRepository repository;
    private final ParkingConfigurationJpaMapper mapper;

    @Override
    public Zone save(Zone value) {
        return mapper.toDomain(
                repository.save(mapper.toJpa(value))
        );
    }

    @Override
    public Optional<Zone> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Zone> findByParkingLotId(Long parkingLotId) {
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

package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.LevelPersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Level;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers.ParkingConfigurationJpaMapper;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories.LevelJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LevelPersistenceAdapter
        implements LevelPersistencePort {

    private final LevelJpaRepository repository;
    private final ParkingConfigurationJpaMapper mapper;

    @Override
    public Level save(Level value) {
        return mapper.toDomain(
                repository.save(mapper.toJpa(value))
        );
    }

    @Override
    public Optional<Level> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Level> findByZoneId(Long zoneId) {
        return repository.findByZoneId(zoneId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByZoneIdAndName(
            Long zoneId,
            String name
    ) {
        return repository.existsByZoneIdAndName(
                zoneId,
                name
        );
    }
}

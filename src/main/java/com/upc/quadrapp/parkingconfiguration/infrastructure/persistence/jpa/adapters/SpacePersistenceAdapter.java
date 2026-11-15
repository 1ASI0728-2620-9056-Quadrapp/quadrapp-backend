package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.SpacePersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Space;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers.ParkingConfigurationJpaMapper;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories.SpaceJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpacePersistenceAdapter
        implements SpacePersistencePort {

    private final SpaceJpaRepository repository;
    private final ParkingConfigurationJpaMapper mapper;

    @Override
    public Space save(Space value) {
        return mapper.toDomain(
                repository.save(mapper.toJpa(value))
        );
    }

    @Override
    public Optional<Space> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Space> findByIdForOccupancyUpdate(Long id) {
        return repository.findByIdForOccupancyUpdate(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Space> findByLevelId(Long levelId) {
        return repository.findByLevelId(levelId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByLevelIdAndLabel(
            Long levelId,
            String label
    ) {
        return repository.existsByLevelIdAndLabel(
                levelId,
                label
        );
    }
}

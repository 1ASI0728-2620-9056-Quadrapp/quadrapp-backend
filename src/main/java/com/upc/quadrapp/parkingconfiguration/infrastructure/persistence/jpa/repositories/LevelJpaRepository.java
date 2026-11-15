package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.LevelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LevelJpaRepository
        extends JpaRepository<LevelJpaEntity, Long> {

    List<LevelJpaEntity> findByZoneId(Long zoneId);

    boolean existsByZoneIdAndName(
            Long zoneId,
            String name
    );
}

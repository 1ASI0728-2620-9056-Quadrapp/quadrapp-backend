package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.SpaceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceJpaRepository
        extends JpaRepository<SpaceJpaEntity, Long> {

    List<SpaceJpaEntity> findByLevelId(Long levelId);

    boolean existsByLevelIdAndLabel(
            Long levelId,
            String label
    );
}

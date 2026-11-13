package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.SpaceJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceJpaRepository
        extends JpaRepository<SpaceJpaEntity, Long> {

    List<SpaceJpaEntity> findByLevelId(Long levelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select space from SpaceJpaEntity space where space.id = :id")
    Optional<SpaceJpaEntity> findByIdForOccupancyUpdate(@Param("id") Long id);

    boolean existsByLevelIdAndLabel(
            Long levelId,
            String label
    );
}

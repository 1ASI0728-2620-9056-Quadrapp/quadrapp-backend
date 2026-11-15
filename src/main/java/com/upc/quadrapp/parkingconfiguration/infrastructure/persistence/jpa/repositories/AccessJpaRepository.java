package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.AccessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessJpaRepository
        extends JpaRepository<AccessJpaEntity, Long> {

    List<AccessJpaEntity> findByParkingLotId(
            Long parkingLotId
    );

    boolean existsByParkingLotIdAndName(
            Long parkingLotId,
            String name
    );
}

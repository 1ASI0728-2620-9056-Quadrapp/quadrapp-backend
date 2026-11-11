package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.ZoneJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneJpaRepository
        extends JpaRepository<ZoneJpaEntity, Long> {

    List<ZoneJpaEntity> findByParkingLotId(
            Long parkingLotId
    );

    boolean existsByParkingLotIdAndName(
            Long parkingLotId,
            String name
    );
}

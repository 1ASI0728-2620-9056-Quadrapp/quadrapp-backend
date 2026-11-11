package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.ParkingLotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLotJpaRepository
        extends JpaRepository<ParkingLotJpaEntity, Long> {

    List<ParkingLotJpaEntity> findByInstitutionId(
            Long institutionId
    );

    boolean existsByInstitutionIdAndName(
            Long institutionId,
            String name
    );
}

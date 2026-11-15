package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.ParkingLotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParkingLotJpaRepository
        extends JpaRepository<ParkingLotJpaEntity, Long> {

    List<ParkingLotJpaEntity> findByInstitutionId(
            Long institutionId
    );

    @Query("SELECT lot.id FROM ParkingLotJpaEntity lot WHERE lot.active = true ORDER BY lot.id")
    List<Long> findActiveIds();

    boolean existsByInstitutionIdAndName(
            Long institutionId,
            String name
    );
}

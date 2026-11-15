package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceJpaRepository extends JpaRepository<DeviceJpaEntity, Long> {
    Optional<DeviceJpaEntity> findByHardwareId(String hardwareId);
    List<DeviceJpaEntity> findByInstitutionIdAndParkingLotId(Long institutionId, Long parkingLotId);
    boolean existsByHardwareId(String hardwareId);
    boolean existsBySpaceIdAndIdNot(Long spaceId, Long excludedId);
    boolean existsBySpaceId(Long spaceId);
}

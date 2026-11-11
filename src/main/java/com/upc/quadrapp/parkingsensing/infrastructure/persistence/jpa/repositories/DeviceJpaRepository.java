package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceJpaEntity;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceJpaRepository extends JpaRepository<DeviceJpaEntity, Long> {
    Optional<DeviceJpaEntity> findByHardwareId(String hardwareId);
    List<DeviceJpaEntity> findByInstitutionIdAndParkingLotId(Long institutionId, Long parkingLotId);
    boolean existsByHardwareId(String hardwareId);
    boolean existsBySpaceIdAndStatusAndIdNot(Long spaceId, DeviceStatus status, Long excludedId);
    boolean existsBySpaceIdAndStatus(Long spaceId, DeviceStatus status);
}
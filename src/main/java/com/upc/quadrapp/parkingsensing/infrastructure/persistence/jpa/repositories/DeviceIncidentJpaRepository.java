package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceIncidentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import java.util.List;

public interface DeviceIncidentJpaRepository extends JpaRepository<DeviceIncidentJpaEntity, Long> {
    List<DeviceIncidentJpaEntity> findByInstitutionIdOrderByOccurredAtDesc(Long institutionId);

    boolean existsByDeviceIdAndTypeAndResolvedAtIsNull(Long deviceId, IncidentType type);
    List<DeviceIncidentJpaEntity> findByInstitutionIdAndResolvedAtIsNullOrderByOccurredAtDesc(Long institutionId);
    List<DeviceIncidentJpaEntity> findByInstitutionIdAndParkingLotIdOrderByOccurredAtDesc(Long institutionId, Long parkingLotId);
    List<DeviceIncidentJpaEntity> findByInstitutionIdAndParkingLotIdAndResolvedAtIsNullOrderByOccurredAtDesc(Long institutionId, Long parkingLotId);
}
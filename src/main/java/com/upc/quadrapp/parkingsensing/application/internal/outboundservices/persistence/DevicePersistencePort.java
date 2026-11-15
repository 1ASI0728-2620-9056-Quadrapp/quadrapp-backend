package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import java.util.List;
import java.util.Optional;

public interface DevicePersistencePort {
    Device save(Device device);
    Optional<Device> findById(Long id);
    Optional<Device> findByHardwareId(String hardwareId);
    List<Device> findByInstitutionIdAndParkingLotId(Long institutionId, Long parkingLotId);
    boolean existsByHardwareId(String hardwareId);
    boolean existsAssignedSpaceExcludingDevice(Long spaceId, Long excludedDeviceId);
}
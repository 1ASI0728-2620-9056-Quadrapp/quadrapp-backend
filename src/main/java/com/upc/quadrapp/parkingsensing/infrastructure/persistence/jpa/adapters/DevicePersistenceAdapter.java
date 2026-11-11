package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.mappers.ParkingSensingJpaMapper;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories.DeviceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DevicePersistenceAdapter implements DevicePersistencePort {
    private final DeviceJpaRepository repository;
    private final ParkingSensingJpaMapper mapper;

    @Override
    public Device save(Device device) {
        return mapper.toDomain(repository.save(mapper.toJpa(device)));
    }

    @Override
    public Optional<Device> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Device> findByHardwareId(String hardwareId) {
        return repository.findByHardwareId(hardwareId).map(mapper::toDomain);
    }

    @Override
    public List<Device> findByInstitutionIdAndParkingLotId(Long institutionId, Long parkingLotId) {
        return repository.findByInstitutionIdAndParkingLotId(institutionId, parkingLotId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByHardwareId(String hardwareId) {
        return repository.existsByHardwareId(hardwareId);
    }

    @Override
    public boolean existsAssignedSpaceExcludingDevice(Long spaceId, Long excludedDeviceId) {
        return excludedDeviceId == null
                ? repository.existsBySpaceIdAndStatus(spaceId, DeviceStatus.ASSIGNED)
                : repository.existsBySpaceIdAndStatusAndIdNot(
                spaceId, DeviceStatus.ASSIGNED, excludedDeviceId);
    }
}
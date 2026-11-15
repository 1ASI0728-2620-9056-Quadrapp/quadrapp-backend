package com.upc.quadrapp.parkingsensing.application.internal.queryservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.ParkingSensingAuthorizationPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.queries.*;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceQueryService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DeviceQueryServiceImpl implements DeviceQueryService {
    private final DevicePersistencePort devices;
    private final ParkingConfigurationLookupPort configuration;
    private final ParkingSensingAuthorizationPort authorization;

    public DeviceQueryServiceImpl(DevicePersistencePort devices,
                                  ParkingConfigurationLookupPort configuration,
                                  ParkingSensingAuthorizationPort authorization) {
        this.devices = Objects.requireNonNull(devices);
        this.configuration = Objects.requireNonNull(configuration);
        this.authorization = Objects.requireNonNull(authorization);
    }

    @Override
    public Optional<Device> handle(GetDeviceByIdQuery query) {
        Optional<Device> result = devices.findById(query.deviceId());
        result.ifPresent(device -> authorization.requireInstitutionRead(device.getInstitutionId()));
        return result;
    }

    @Override
    public Optional<Device> handle(GetDeviceByHardwareIdQuery query) {
        Optional<Device> result = devices.findByHardwareId(query.hardwareId());
        result.ifPresent(device -> authorization.requireInstitutionRead(device.getInstitutionId()));
        return result;
    }

    @Override
    public List<Device> handle(GetDevicesByParkingLotQuery query) {
        authorization.requireInstitutionRead(query.institutionId());
        configuration.requireParkingLot(query.institutionId(), query.parkingLotId());
        return devices.findByInstitutionIdAndParkingLotId(query.institutionId(), query.parkingLotId());
    }
}
package com.upc.quadrapp.parkingsensing.application.internal.commandservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.ParkingSensingAuthorizationPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.SensorFeedStatePort;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.commands.*;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceCommandService;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Application use cases; register as a Spring bean after Infrastructure is ready. */
public final class DeviceCommandServiceImpl implements DeviceCommandService {
    private final DevicePersistencePort devices;
    private final ParkingConfigurationLookupPort configuration;
    private final ParkingSensingAuthorizationPort authorization;
    private final SensorFeedStatePort feeds;
    private final Clock clock;

    public DeviceCommandServiceImpl(DevicePersistencePort devices,
                                    ParkingConfigurationLookupPort configuration,
                                    ParkingSensingAuthorizationPort authorization,
                                    SensorFeedStatePort feeds, Clock clock) {
        this.devices = Objects.requireNonNull(devices);
        this.configuration = Objects.requireNonNull(configuration);
        this.authorization = Objects.requireNonNull(authorization);
        this.feeds = Objects.requireNonNull(feeds);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Device handle(RegisterDeviceCommand command) {
        authorization.requireInstitutionWrite(command.institutionId());
        configuration.requireParkingLot(command.institutionId(), command.parkingLotId());
        if (devices.existsByHardwareId(command.hardwareId())) {
            throw new IllegalStateException("Hardware identifier is already registered.");
        }
        return devices.save(Device.register(command.institutionId(), command.parkingLotId(),
                command.hardwareId(), command.type(), command.reportIntervalSeconds(), Instant.now(clock)));
    }

    @Override
    public Device handle(AssignDeviceCommand command) {
        Device device = requireDevice(command.deviceId());
        authorization.requireInstitutionWrite(device.getInstitutionId());
        if (command.spaceId() != null) {
            configuration.requireSpace(device.getInstitutionId(), device.getParkingLotId(), command.spaceId());
            if (devices.existsAssignedSpaceExcludingDevice(command.spaceId(), device.getId())) {
                throw new IllegalStateException("Parking space already has an assigned sensor.");
            }
            boolean changed = !command.spaceId().equals(device.getSpaceId());
            device.assignToSpace(command.spaceId());
            Device saved = devices.save(device);
            if (changed) feeds.reset(saved.getId());
            return saved;
        }
        configuration.requireAccess(device.getInstitutionId(), device.getParkingLotId(), command.accessId());
        boolean changed = !command.accessId().equals(device.getAccessId());
        device.assignToAccess(command.accessId());
        Device saved = devices.save(device);
        if (changed) feeds.reset(saved.getId());
        return saved;
    }

    @Override
    public Device handle(DecommissionDeviceCommand command) {
        Device device = requireDevice(command.deviceId());
        authorization.requireInstitutionWrite(device.getInstitutionId());
        device.decommission();
        Device saved = devices.save(device);
        feeds.deactivate(saved.getId());
        return saved;
    }

    private Device requireDevice(Long id) {
        return devices.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Device not found."));
    }
}
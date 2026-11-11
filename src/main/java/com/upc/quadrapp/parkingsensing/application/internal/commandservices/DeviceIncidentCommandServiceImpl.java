package com.upc.quadrapp.parkingsensing.application.internal.commandservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.ParkingSensingAuthorizationPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.*;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.commands.*;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentCommandService;
import java.util.Objects;

public final class DeviceIncidentCommandServiceImpl implements DeviceIncidentCommandService {
    private final DeviceIncidentPersistencePort incidents;
    private final DevicePersistencePort devices;
    private final ParkingConfigurationLookupPort configuration;
    private final ParkingSensingAuthorizationPort authorization;

    public DeviceIncidentCommandServiceImpl(DeviceIncidentPersistencePort incidents,
                                            DevicePersistencePort devices,
                                            ParkingConfigurationLookupPort configuration,
                                            ParkingSensingAuthorizationPort authorization) {
        this.incidents = Objects.requireNonNull(incidents);
        this.devices = Objects.requireNonNull(devices);
        this.configuration = Objects.requireNonNull(configuration);
        this.authorization = Objects.requireNonNull(authorization);
    }

    @Override
    public DeviceIncident handle(ReportDeviceIncidentCommand command) {
        authorization.requireInstitutionWrite(command.institutionId());
        if (command.parkingLotId() != null) {
            configuration.requireParkingLot(command.institutionId(), command.parkingLotId());
        }
        if (command.deviceId() != null) {
            Device device = devices.findById(command.deviceId()).orElseThrow(
                    () -> new IllegalArgumentException("Device not found."));
            if (!device.getInstitutionId().equals(command.institutionId())
                    || !device.getHardwareId().equals(command.hardwareId())
                    || (command.parkingLotId() != null
                    && !device.getParkingLotId().equals(command.parkingLotId()))) {
                throw new IllegalArgumentException("Incident device does not match the institution/parking lot.");
            }
        }
        return incidents.save(DeviceIncident.open(command.institutionId(), command.parkingLotId(),
                command.deviceId(), command.hardwareId(), command.type(),
                command.details(), command.occurredAt()));
    }

    @Override
    public DeviceIncident handle(ResolveDeviceIncidentCommand command) {
        DeviceIncident incident = incidents.findById(command.incidentId()).orElseThrow(
                () -> new IllegalArgumentException("Incident not found."));
        authorization.requireIncidentResolver(command.actorUserId(), incident.getInstitutionId());
        incident.resolve(command.resolvedAt());
        return incidents.save(incident);
    }
}
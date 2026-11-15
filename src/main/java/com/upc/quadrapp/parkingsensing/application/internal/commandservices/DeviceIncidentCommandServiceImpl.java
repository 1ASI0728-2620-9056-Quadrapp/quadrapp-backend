package com.upc.quadrapp.parkingsensing.application.internal.commandservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.ParkingSensingAuthorizationPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DeviceIncidentPersistencePort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ReportDeviceIncidentCommand;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ResolveDeviceIncidentCommand;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.IncidentType;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceIncidentCommandService;

import java.util.Objects;

public final class DeviceIncidentCommandServiceImpl implements DeviceIncidentCommandService {
    private final DeviceIncidentPersistencePort incidents;
    private final DevicePersistencePort devices;
    private final ParkingConfigurationLookupPort configuration;
    private final ParkingSensingAuthorizationPort authorization;

    public DeviceIncidentCommandServiceImpl(
            DeviceIncidentPersistencePort incidents,
            DevicePersistencePort devices,
            ParkingConfigurationLookupPort configuration,
            ParkingSensingAuthorizationPort authorization
    ) {
        this.incidents = Objects.requireNonNull(incidents);
        this.devices = Objects.requireNonNull(devices);
        this.configuration = Objects.requireNonNull(configuration);
        this.authorization = Objects.requireNonNull(authorization);
    }

    @Override
    public DeviceIncident handle(ReportDeviceIncidentCommand command) {
        authorization.requireInstitutionWrite(
                command.institutionId()
        );

        configuration.requireParkingLot(
                command.institutionId(),
                command.parkingLotId()
        );

        if (command.type() != IncidentType.USER_REPORT ||
                command.category() == null ||
                command.reportedBy() == null) {
            throw new IllegalArgumentException(
                    "Administrative incident reports must be USER_REPORT with category and author."
            );
        }

        String verifiedHardwareId = command.hardwareId();

        if (command.deviceId() != null) {
            Device device = devices.findById(command.deviceId())
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Device not found."
                            )
                    );

            if (!device.getInstitutionId().equals(
                    command.institutionId()
            ) ||
                    (verifiedHardwareId != null &&
                            !device.getHardwareId().equals(
                                    verifiedHardwareId
                            )) ||
                    !device.getParkingLotId().equals(
                            command.parkingLotId()
                    )) {
                throw new IllegalArgumentException(
                        "Incident device does not match the institution or parking lot."
                );
            }

            verifiedHardwareId = device.getHardwareId();
        }

        if (verifiedHardwareId == null ||
                verifiedHardwareId.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident requires a device ID or hardware ID."
            );
        }

        return incidents.save(
                DeviceIncident.open(
                        command.institutionId(),
                        command.parkingLotId(),
                        command.deviceId(),
                        verifiedHardwareId,
                        command.type(),
                        command.category(),
                        command.reportedBy(),
                        command.details(),
                        command.occurredAt()
                )
        );
    }

    @Override
    public DeviceIncident handle(ResolveDeviceIncidentCommand command) {
        DeviceIncident incident = incidents.findById(
                command.incidentId()
        ).orElseThrow(
                () -> new IllegalArgumentException(
                        "Incident not found."
                )
        );

        authorization.requireIncidentResolver(
                command.actorUserId(),
                incident.getInstitutionId()
        );

        incident.resolve(
                command.actorUserId(),
                command.resolvedAt()
        );

        return incidents.save(incident);
    }
}

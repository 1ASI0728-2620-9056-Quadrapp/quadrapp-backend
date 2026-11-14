package com.upc.quadrapp.parkingsensing.application.internal.commandservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events.SensingEventPublisherPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.*;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.*;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;

import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class SensorReadingCommandServiceImpl implements SensorReadingCommandService {

    private final TrustedReadingSourcePort trustedSources;
    private final ReadingProcessingPort processing;
    private final DevicePersistencePort devices;
    private final DeviceIncidentPersistencePort incidents;
    private final ParkingConfigurationLookupPort configuration;
    private final SensorFeedStatePort feeds;
    private final SensingEventPublisherPort events;
    private final Clock clock;

    public SensorReadingCommandServiceImpl(
            TrustedReadingSourcePort trustedSources,
            ReadingProcessingPort processing,
            DevicePersistencePort devices,
            DeviceIncidentPersistencePort incidents,
            ParkingConfigurationLookupPort configuration,
            SensorFeedStatePort feeds,
            SensingEventPublisherPort events,
            Clock clock
    ) {
        this.trustedSources = Objects.requireNonNull(trustedSources);
        this.processing = Objects.requireNonNull(processing);
        this.devices = Objects.requireNonNull(devices);
        this.incidents = Objects.requireNonNull(incidents);
        this.configuration = Objects.requireNonNull(configuration);
        this.feeds = Objects.requireNonNull(feeds);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Saves the reading and publishes its domain events in one transaction (transactional outbox).
     */
    @Override
    @Transactional
    public SensorReadingResult handle(
            ProcessSensorReadingCommand command
    ) {
        SensorReading reading = command.reading();

        trustedSources.requireTrustedSource(
                command.institutionId(),
                reading
        );

        return processing.processOnce(
                command.institutionId(),
                reading.hardwareId(),
                reading.eventId(),
                () -> processReading(
                        command.institutionId(),
                        reading
                )
        );
    }

    private SensorReadingResult processReading(
            Long institutionId,
            SensorReading reading
    ) {
        Device device = devices.findByHardwareId(
                reading.hardwareId()
        ).orElse(null);

        if (device == null) {
            throw new IllegalArgumentException(
                    "Unknown device."
            );
        }

        if (!device.getInstitutionId().equals(institutionId)) {
            throw new SecurityException(
                    "Device belongs to another institution."
            );
        }

        if (device.getStatus() == DeviceStatus.DECOMMISSIONED) {
            throw new IllegalStateException(
                    "Decommissioned device cannot report."
            );
        }

        configuration.requireParkingLot(
                institutionId,
                device.getParkingLotId()
        );

        validateReadingKind(device, reading);

        if (!device.recordReport(reading.readingAt())) {
            return new SensorReadingResult(
                    ReadingOutcome.OUT_OF_ORDER,
                    null,
                    null
            );
        }

        devices.save(device);

        if (reading.kind() == ReadingKind.HEARTBEAT) {
            return new SensorReadingResult(
                    ReadingOutcome.HEARTBEAT_ACCEPTED,
                    null,
                    null
            );
        }

        if (reading.kind() == ReadingKind.PASS) {
            events.publishPassageDetection(
                    institutionId,
                    device.getParkingLotId(),
                    device.getAccessId(),
                    device.getId(),
                    reading.eventId(),
                    reading.direction(),
                    reading.readingAt()
            );

            return new SensorReadingResult(
                    ReadingOutcome.PASSAGE_RECORDED,
                    null,
                    reading.direction()
            );
        }

        int minimumSeconds = configuration.minimumDetectionSeconds(
                institutionId,
                device.getParkingLotId(),
                reading.state()
        );

        if (minimumSeconds <= 0) {
            throw new IllegalStateException(
                    "Invalid minimum detection time."
            );
        }

        SensorReadingResult result = feeds.accept(
                device.getId(),
                minimumSeconds,
                reading
        );

        if (result.outcome() == ReadingOutcome.CONFIRMED) {
            events.publishSpaceDetection(
                    institutionId,
                    device.getParkingLotId(),
                    device.getSpaceId(),
                    device.getId(),
                    reading.eventId(),
                    result.confirmedState(),
                    reading.readingAt()
            );
        }

        return result;
    }

    private void validateReadingKind(
            Device device,
            SensorReading reading
    ) {
        boolean valid = switch (reading.kind()) {

            case SPACE ->
                    device.getType() == DeviceType.SPACE_SENSOR &&
                            device.getStatus() != DeviceStatus.DECOMMISSIONED &&
                            device.getSpaceId() != null;

            case PASS ->
                    device.getType() == DeviceType.PASS_SENSOR &&
                            device.getStatus() != DeviceStatus.DECOMMISSIONED &&
                            device.getAccessId() != null;

            case HEARTBEAT -> true;
        };

        if (!valid) {
            registerIncident(
                    device.getInstitutionId(),
                    device.getParkingLotId(),
                    device.getId(),
                    device.getHardwareId(),
                    IncidentType.UNMAPPED_SENSOR,
                    "Reading kind does not match device assignment."
            );

            throw new IllegalStateException(
                    "Reading not allowed for device assignment."
            );
        }
    }

    private void registerIncident(
            Long institutionId,
            Long parkingLotId,
            Long deviceId,
            String hardwareId,
            IncidentType type,
            String details
    ) {
        incidents.save(
                DeviceIncident.open(
                        institutionId,
                        parkingLotId,
                        deviceId,
                        hardwareId,
                        type,
                        null,
                        null,
                        details,
                        Instant.now(clock)
                )
        );
    }
}

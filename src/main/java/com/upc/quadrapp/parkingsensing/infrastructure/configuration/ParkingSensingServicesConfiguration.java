package com.upc.quadrapp.parkingsensing.infrastructure.configuration;

import com.upc.quadrapp.parkingsensing.application.internal.commandservices.*;
import com.upc.quadrapp.parkingsensing.application.internal.queryservices.*;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.*;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.*;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.*;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.*;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.events.*;
import com.upc.quadrapp.parkingsensing.domain.services.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;
import java.time.Clock;

@Configuration
public class ParkingSensingServicesConfiguration {
    @Bean
    public Clock parkingSensingClock() {
        return Clock.systemUTC();
    }

    @Bean
    public DeviceCommandService deviceCommandService(
            DevicePersistencePort devices, ParkingConfigurationLookupPort configuration,
            ParkingSensingAuthorizationPort authorization, SensorFeedStatePort feeds, Clock clock) {
        return new DeviceCommandServiceImpl(devices, configuration, authorization, feeds, clock);
    }

    @Bean
    public DeviceQueryService deviceQueryService(
            DevicePersistencePort devices, ParkingConfigurationLookupPort configuration,
            ParkingSensingAuthorizationPort authorization) {
        return new DeviceQueryServiceImpl(devices, configuration, authorization);
    }

    @Bean
    public DeviceIncidentCommandService deviceIncidentCommandService(
            DeviceIncidentPersistencePort incidents, DevicePersistencePort devices,
            ParkingConfigurationLookupPort configuration, ParkingSensingAuthorizationPort authorization) {
        return new DeviceIncidentCommandServiceImpl(incidents, devices, configuration, authorization);
    }

    @Bean
    public DeviceIncidentQueryService deviceIncidentQueryService(
            DeviceIncidentPersistencePort incidents, ParkingConfigurationLookupPort configuration,
            ParkingSensingAuthorizationPort authorization) {
        return new DeviceIncidentQueryServiceImpl(incidents, configuration, authorization);
    }

    @Bean
    public SensorReadingCommandService sensorReadingCommandService(
            TrustedReadingSourcePort trustedSources, ReadingProcessingPort processing,
            DevicePersistencePort devices, DeviceIncidentPersistencePort incidents,
            ParkingConfigurationLookupPort configuration, SensorFeedStatePort feeds,
            SensingEventPublisherPort events, TransactionOperations transactions, Clock clock) {
        return new SensorReadingCommandServiceImpl(trustedSources, processing, devices,
                incidents, configuration, feeds, events, transactions, clock);
    }
}
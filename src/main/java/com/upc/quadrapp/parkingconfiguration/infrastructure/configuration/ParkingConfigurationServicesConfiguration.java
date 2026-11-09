package com.upc.quadrapp.parkingconfiguration.infrastructure.configuration;

import com.upc.quadrapp.parkingconfiguration.application.internal.commandservices.CalendarEventCommandServiceImpl;
import com.upc.quadrapp.parkingconfiguration.application.internal.commandservices.ParkingConfigurationCommandServiceImpl;

import com.upc.quadrapp.parkingconfiguration.application.internal.queryservices.CalendarEventQueryServiceImpl;
import com.upc.quadrapp.parkingconfiguration.application.internal.queryservices.ParkingConfigurationQueryServiceImpl;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.*;

import com.upc.quadrapp.parkingconfiguration.domain.services.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParkingConfigurationServicesConfiguration {

    @Bean
    public ParkingConfigurationCommandService parkingConfigurationCommandService(
            ParkingLotPersistencePort lots,
            ZonePersistencePort zones,
            LevelPersistencePort levels,
            SpacePersistencePort spaces,
            AccessPersistencePort accesses,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        return new ParkingConfigurationCommandServiceImpl(
                lots,
                zones,
                levels,
                spaces,
                accesses,
                authorization
        );
    }

    @Bean
    public ParkingConfigurationQueryService parkingConfigurationQueryService(
            ParkingLotPersistencePort lots,
            ZonePersistencePort zones,
            LevelPersistencePort levels,
            SpacePersistencePort spaces,
            AccessPersistencePort accesses,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        return new ParkingConfigurationQueryServiceImpl(
                lots,
                zones,
                levels,
                spaces,
                accesses,
                authorization
        );
    }

    @Bean
    public CalendarEventCommandService calendarEventCommandService(
            CalendarEventPersistencePort events,
            ParkingLotPersistencePort lots,
            ZonePersistencePort zones,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        return new CalendarEventCommandServiceImpl(
                events,
                lots,
                zones,
                authorization
        );
    }

    @Bean
    public CalendarEventQueryService calendarEventQueryService(
            CalendarEventPersistencePort events,
            ParkingConfigurationAuthorizationPort authorization
    ) {
        return new CalendarEventQueryServiceImpl(
                events,
                authorization
        );
    }
}

package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.flyway;

import com.upc.quadrapp.shared.infrastructure.persistence.flyway.ModuleSchemaMigrations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migrates the {@code configuration} schema owned by the Parking Configuration bounded context.
 */
@Configuration(proxyBeanMethods = false)
public class ParkingConfigurationSchemaMigrationConfiguration {

    public static final String SCHEMA = "configuration";

    /** Tables created by this module's migrations. */
    public static final List<String> TABLES = List.of("accesses", "calendar_events", "levels", "parking_lots", "spaces", "zones");

    @Bean
    public FlywayMigrationInitializer parkingConfigurationSchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

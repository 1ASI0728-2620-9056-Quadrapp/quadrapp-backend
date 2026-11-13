package com.upc.quadrapp.parkingsensing.infrastructure.persistence.flyway;

import com.upc.quadrapp.shared.infrastructure.persistence.flyway.ModuleSchemaMigrations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migrates the {@code sensing} schema owned by the Parking Sensing bounded context.
 */
@Configuration(proxyBeanMethods = false)
public class ParkingSensingSchemaMigrationConfiguration {

    public static final String SCHEMA = "sensing";

    /** Tables created by this module's migrations. */
    public static final List<String> TABLES = List.of("device_incidents", "devices");

    @Bean
    public FlywayMigrationInitializer parkingSensingSchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

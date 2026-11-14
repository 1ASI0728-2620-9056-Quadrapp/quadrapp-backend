package com.upc.quadrapp.occupancy.infrastructure.persistence.flyway;

import com.upc.quadrapp.shared.infrastructure.persistence.flyway.ModuleSchemaMigrations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migrates the {@code occupancy} schema owned by the Occupancy bounded context.
 */
@Configuration(proxyBeanMethods = false)
public class OccupancySchemaMigrationConfiguration {

    public static final String SCHEMA = "occupancy";

    /** Tables created by this module's migrations. */
    public static final List<String> TABLES = List.of("occupancy_snapshots", "sensor_events");

    @Bean
    public FlywayMigrationInitializer occupancySchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

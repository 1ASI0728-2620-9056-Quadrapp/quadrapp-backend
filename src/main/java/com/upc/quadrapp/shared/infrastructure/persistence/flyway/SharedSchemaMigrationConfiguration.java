package com.upc.quadrapp.shared.infrastructure.persistence.flyway;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migrates the {@code shared} schema: infrastructure tables used by every module, such as the
 * Spring Modulith event publication registry and the ShedLock locks. No bounded context stores domain data here.
 */
@Configuration(proxyBeanMethods = false)
public class SharedSchemaMigrationConfiguration {

    public static final String SCHEMA = "shared";

    /** Tables created by this schema's migrations. */
    public static final List<String> TABLES = List.of("event_publication", "shedlock");

    @Bean
    public FlywayMigrationInitializer sharedSchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

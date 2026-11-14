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

    /**
     * Legacy tables to adopt from {@code public}: none, because this schema was created by its
     * own migrations from the start (event_publication in V1, shedlock in V2).
     */
    public static final List<String> TABLES = List.of();

    @Bean
    public FlywayMigrationInitializer sharedSchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

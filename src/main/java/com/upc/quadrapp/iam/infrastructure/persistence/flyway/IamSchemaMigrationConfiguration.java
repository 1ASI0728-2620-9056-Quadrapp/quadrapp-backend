package com.upc.quadrapp.iam.infrastructure.persistence.flyway;

import com.upc.quadrapp.shared.infrastructure.persistence.flyway.ModuleSchemaMigrations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migrates the {@code iam} schema owned by the IAM bounded context.
 */
@Configuration(proxyBeanMethods = false)
public class IamSchemaMigrationConfiguration {

    public static final String SCHEMA = "iam";

    /** Tables created by this module's migrations. */
    public static final List<String> TABLES = List.of("institution_domains", "institutions", "refresh_tokens", "users");

    @Bean
    public FlywayMigrationInitializer iamSchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

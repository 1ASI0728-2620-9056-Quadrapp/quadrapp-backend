package com.upc.quadrapp.prediction.infrastructure.persistence.flyway;

import com.upc.quadrapp.shared.infrastructure.persistence.flyway.ModuleSchemaMigrations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migrates the {@code prediction} schema owned by the Prediction bounded context.
 */
@Configuration(proxyBeanMethods = false)
public class PredictionSchemaMigrationConfiguration {

    public static final String SCHEMA = "prediction";

    /** Tables created by this module's migrations. */
    public static final List<String> TABLES = List.of("forecasts");

    @Bean
    public FlywayMigrationInitializer predictionSchemaMigration(DataSource dataSource) {
        return ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES);
    }
}

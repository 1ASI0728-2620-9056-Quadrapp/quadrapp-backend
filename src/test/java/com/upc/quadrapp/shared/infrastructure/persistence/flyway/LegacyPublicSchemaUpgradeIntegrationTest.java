package com.upc.quadrapp.shared.infrastructure.persistence.flyway;

import com.upc.quadrapp.iam.infrastructure.persistence.flyway.IamSchemaMigrationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A database created before module schemas existed keeps the IAM tables in {@code public}.
 * Migrating it must keep the existing data instead of hiding it behind new empty tables.
 */
@Testcontainers
class LegacyPublicSchemaUpgradeIntegrationTest {

    private static final String SCHEMA = IamSchemaMigrationConfiguration.SCHEMA;
    private static final List<String> TABLES = IamSchemaMigrationConfiguration.TABLES;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private PGSimpleDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void startFromAnEmptyDatabase() {
        dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("drop schema if exists " + SCHEMA + " cascade");
        jdbcTemplate.execute("drop schema public cascade");
        jdbcTemplate.execute("create schema public");
    }

    @Test
    void movesLegacyTablesWithTheirDataIntoTheModuleSchema() throws Exception {
        createLegacyIamTablesInPublic();
        jdbcTemplate.update("insert into public.institutions (name) values ('UPC')");

        ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES).afterPropertiesSet();

        assertThat(jdbcTemplate.queryForList("select name from iam.institutions", String.class))
                .containsExactly("UPC");
        assertThat(tablesIn("public")).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "select type from iam.flyway_schema_history where version = '1'", String.class))
                .containsExactly("BASELINE");
        jdbcTemplate.update("insert into iam.institutions (name) values ('UNI')");
        assertThat(jdbcTemplate.queryForObject("select count(*) from iam.institutions", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void refusesToStartWhenOnlySomeLegacyTablesExist() {
        jdbcTemplate.execute("create table public.institutions (id bigint primary key, name varchar(150))");
        jdbcTemplate.update("insert into public.institutions values (1, 'UPC')");

        assertThatThrownBy(() -> ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("partial set of tables");
        assertThat(jdbcTemplate.queryForObject("select count(*) from public.institutions", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void refusesToStartWhenTablesExistInBothSchemas() throws Exception {
        ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES).afterPropertiesSet();
        createLegacyIamTablesInPublic();

        assertThatThrownBy(() -> ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exist in both the public and the iam schema");
    }

    private void createLegacyIamTablesInPublic() throws Exception {
        String migration = new ClassPathResource("db/migration/iam/V1__create_initial_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute(migration.replace("iam.", "public."));
    }

    private List<String> tablesIn(String schema) {
        return jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = ? and table_type = 'BASE TABLE'",
                String.class, schema);
    }
}

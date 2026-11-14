package com.upc.quadrapp.shared.infrastructure.persistence.flyway;

import com.upc.quadrapp.iam.infrastructure.persistence.flyway.IamSchemaMigrationConfiguration;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.flyway.ParkingConfigurationSchemaMigrationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
        jdbcTemplate.execute("drop schema if exists " + ParkingConfigurationSchemaMigrationConfiguration.SCHEMA + " cascade");
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
    void dropsForeignKeysThatCrossModuleSchemasAndKeepsTheOthers() throws Exception {
        createLegacyIamTablesInPublic();
        createLegacyTablesInPublic("configuration");
        jdbcTemplate.execute("alter table public.parking_lots add constraint fk_parking_lots_institution "
                + "foreign key (institution_id) references public.institutions(id)");
        jdbcTemplate.execute("create table public.push_tokens (id bigint primary key, "
                + "user_id bigint constraint fk_push_tokens_user references public.users(id))");

        ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES).afterPropertiesSet();
        ModuleSchemaMigrations.forSchema(dataSource, ParkingConfigurationSchemaMigrationConfiguration.SCHEMA,
                ParkingConfigurationSchemaMigrationConfiguration.TABLES).afterPropertiesSet();

        assertThat(jdbcTemplate.queryForList(
                "select con.conname from pg_constraint con "
                        + "join pg_class src on src.oid = con.conrelid "
                        + "join pg_class dst on dst.oid = con.confrelid "
                        + "where con.contype = 'f' and src.relnamespace <> dst.relnamespace",
                String.class)).isEmpty();
        assertThat(foreignKeysWithin(SCHEMA)).containsExactlyInAnyOrder(
                "fk_institution_domains_institution", "fk_institution_domains_verifier",
                "fk_refresh_tokens_user", "fk_users_institution", "fk_users_inviter");
    }

    @Test
    void theSharedSchemaHasNoLegacyTablesToAdopt() throws Exception {
        jdbcTemplate.execute("drop schema if exists " + SharedSchemaMigrationConfiguration.SCHEMA + " cascade");
        jdbcTemplate.execute("create table public.event_publication (id uuid primary key)");

        ModuleSchemaMigrations.forSchema(dataSource, SharedSchemaMigrationConfiguration.SCHEMA,
                SharedSchemaMigrationConfiguration.TABLES).afterPropertiesSet();

        assertThat(tablesIn("shared")).contains("event_publication", "shedlock");
        assertThat(tablesIn("public")).containsExactly("event_publication");
    }

    @Test
    void twoReplicasStartingTogetherOnALegacyDatabaseBothSucceed() throws Exception {
        createLegacyIamTablesInPublic();
        jdbcTemplate.update("insert into public.institutions (name) values ('UPC')");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService replicas = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> startups = new ArrayList<>();
            for (int replica = 0; replica < 2; replica++) {
                startups.add(replicas.submit(() -> {
                    start.await();
                    ModuleSchemaMigrations.forSchema(dataSource, SCHEMA, TABLES).afterPropertiesSet();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> startup : startups) {
                startup.get(60, TimeUnit.SECONDS);
            }
        } finally {
            replicas.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForList("select name from iam.institutions", String.class)).containsExactly("UPC");
        assertThat(tablesIn("public")).isEmpty();
    }

    @Test
    void legacyTablesOwnedByAnotherRoleAreNotHiddenBehindNewEmptyTables() throws Exception {
        createLegacyIamTablesInPublic();
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        dropLimitedRole();
        jdbcTemplate.execute("create role limited_app login password 'limited'");
        jdbcTemplate.execute("grant create on database " + database + " to limited_app");
        PGSimpleDataSource limited = new PGSimpleDataSource();
        limited.setUrl(POSTGRES.getJdbcUrl());
        limited.setUser("limited_app");
        limited.setPassword("limited");
        try {
            assertThatThrownBy(() -> ModuleSchemaMigrations.forSchema(limited, SCHEMA, TABLES).afterPropertiesSet())
                    .isInstanceOf(IllegalStateException.class);
            assertThat(tablesIn(SCHEMA)).isEmpty();
        } finally {
            jdbcTemplate.execute("drop schema if exists " + SCHEMA + " cascade");
            dropLimitedRole();
        }
    }

    private void dropLimitedRole() {
        jdbcTemplate.execute("do $$ begin if exists (select from pg_roles where rolname = 'limited_app') then "
                + "execute 'drop owned by limited_app cascade'; execute 'drop role limited_app'; end if; end $$");
    }

    @Test
    void identifiersMayContainDigitsButNotStartWithOne() {
        assertThatCode(() -> ModuleSchemaMigrations.forSchema(dataSource, "module2", List.of("readings_v2")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ModuleSchemaMigrations.forSchema(dataSource, "2module", List.of("readings")))
                .isInstanceOf(IllegalArgumentException.class);
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
        createLegacyTablesInPublic("iam");
    }

    private void createLegacyTablesInPublic(String schema) throws Exception {
        String migration = new ClassPathResource("db/migration/" + schema + "/V1__create_initial_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute(migration.replace(schema + ".", "public."));
    }

    private List<String> foreignKeysWithin(String schema) {
        return jdbcTemplate.queryForList(
                "select con.conname from pg_constraint con "
                        + "join pg_class src on src.oid = con.conrelid "
                        + "join pg_class dst on dst.oid = con.confrelid "
                        + "join pg_namespace ns on ns.oid = src.relnamespace "
                        + "where con.contype = 'f' and ns.nspname = ? and src.relnamespace = dst.relnamespace",
                String.class, schema);
    }

    private List<String> tablesIn(String schema) {
        return jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = ? and table_type = 'BASE TABLE'",
                String.class, schema);
    }
}

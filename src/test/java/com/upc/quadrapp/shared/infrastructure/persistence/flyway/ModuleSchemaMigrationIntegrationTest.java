package com.upc.quadrapp.shared.infrastructure.persistence.flyway;

import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the persistence rules of the modular monolith (DD-01, CON-02):
 * one schema per bounded context, its own migration history and no foreign key across schemas.
 */
@QuadrappIntegrationTest
class ModuleSchemaMigrationIntegrationTest {

    private static final Map<String, List<String>> MODULE_TABLES = Map.of(
            "iam", List.of("institution_domains", "institutions", "refresh_tokens", "users"),
            "configuration", List.of("accesses", "calendar_events", "levels", "parking_lots", "spaces", "zones"),
            "sensing", List.of("device_incidents", "devices"),
            "occupancy", List.of("occupancy_snapshots", "sensor_events"),
            "prediction", List.of("forecasts"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void eachModuleOwnsItsTablesInItsOwnSchema() {
        MODULE_TABLES.forEach((schema, expectedTables) ->
                assertThat(applicationTablesIn(schema))
                        .as("tables in schema %s", schema)
                        .containsExactlyElementsOf(expectedTables));
    }

    @Test
    void noApplicationTableIsCreatedInThePublicSchema() {
        assertThat(applicationTablesIn("public")).isEmpty();
    }

    @Test
    void eachModuleKeepsItsOwnSuccessfulMigrationHistory() {
        MODULE_TABLES.keySet().forEach(schema ->
                assertThat(jdbcTemplate.queryForList(
                        "select version from " + schema + ".flyway_schema_history"
                                + " where version is not null and success order by installed_rank",
                        String.class))
                        .as("applied migrations in schema %s", schema)
                        .containsExactly("1"));
    }

    @Test
    void noForeignKeyCrossesModuleSchemas() {
        List<String> crossSchemaForeignKeys = jdbcTemplate.queryForList("""
                select source_ns.nspname || '.' || fk.conname
                from pg_constraint fk
                join pg_class source on source.oid = fk.conrelid
                join pg_namespace source_ns on source_ns.oid = source.relnamespace
                join pg_class target on target.oid = fk.confrelid
                join pg_namespace target_ns on target_ns.oid = target.relnamespace
                where fk.contype = 'f' and source_ns.nspname <> target_ns.nspname
                """, String.class);
        assertThat(crossSchemaForeignKeys).isEmpty();
    }

    private List<String> applicationTablesIn(String schema) {
        return jdbcTemplate.queryForList("""
                select table_name from information_schema.tables
                where table_schema = ? and table_type = 'BASE TABLE'
                  and table_name <> 'flyway_schema_history'
                order by table_name
                """, String.class, schema);
    }
}

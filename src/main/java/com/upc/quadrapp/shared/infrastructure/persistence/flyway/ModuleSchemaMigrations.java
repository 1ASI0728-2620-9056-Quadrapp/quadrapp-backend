package com.upc.quadrapp.shared.infrastructure.persistence.flyway;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Builds the database migration of one bounded context.
 * <p>
 * Each module owns a PostgreSQL schema and keeps its own migration history inside it,
 * so modules evolve independently and never share tables (DD-01, CON-02).
 * Migrations live in {@code classpath:db/migration/<schema>}.
 * Spring Boot detects every {@link FlywayMigrationInitializer} bean and runs it before JPA
 * validates the mappings, so {@code ddl-auto=validate} always checks a migrated database.
 * <p>
 * Upgrade path: databases created before module schemas existed keep the module tables in
 * {@code public}. Before migrating, those tables (with their data, sequences and constraints)
 * are moved into the module schema and Flyway baselines it at version 1 instead of creating
 * the tables again. A partial or duplicated set of tables stops the startup with an explicit
 * error, so data is never hidden or split silently.
 * <p>
 * Moved tables keep their foreign keys, including those that point to (or come from) tables of
 * another module. Modules only share IDs, never foreign keys (CON-02), so every startup drops
 * the foreign keys that cross this schema's boundary. Fresh databases have none, running it
 * again changes nothing, and two replicas starting together do not fail on the same constraint.
 */
public final class ModuleSchemaMigrations {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleSchemaMigrations.class);
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_]+");
    private static final String LEGACY_SCHEMA = "public";

    private ModuleSchemaMigrations() {
    }

    public static FlywayMigrationInitializer forSchema(DataSource dataSource, String schema, List<String> tables) {
        requireIdentifier(schema);
        tables.forEach(ModuleSchemaMigrations::requireIdentifier);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration/" + schema)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("adopt tables moved from the public schema")
                .load();
        return new FlywayMigrationInitializer(flyway, migration -> {
            adoptLegacyTables(dataSource, schema, tables);
            dropCrossSchemaForeignKeys(dataSource, schema);
            migration.migrate();
        });
    }

    static void adoptLegacyTables(DataSource dataSource, String schema, List<String> tables) {
        try (Connection connection = dataSource.getConnection()) {
            Set<String> legacyTables = existingTables(connection, LEGACY_SCHEMA, tables);
            if (legacyTables.isEmpty()) {
                return;
            }
            Set<String> moduleTables = existingTables(connection, schema, tables);
            if (!moduleTables.isEmpty()) {
                throw new IllegalStateException("Tables " + legacyTables + " exist in both the public and the "
                        + schema + " schema. Reconcile the duplicated data manually before starting the application.");
            }
            if (legacyTables.size() != tables.size()) {
                throw new IllegalStateException("Only " + legacyTables + " of " + tables + " exist in the public schema. "
                        + "A partial set of tables cannot be moved to the " + schema + " schema automatically.");
            }
            moveToSchema(connection, schema, tables);
            LOGGER.warn("Moved legacy tables {} from the public schema to the {} schema", tables, schema);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check the legacy tables of the " + schema + " schema", exception);
        }
    }

    static void dropCrossSchemaForeignKeys(DataSource dataSource, String schema) {
        String query = "select src_ns.nspname, src.relname, con.conname from pg_constraint con "
                + "join pg_class src on src.oid = con.conrelid "
                + "join pg_namespace src_ns on src_ns.oid = src.relnamespace "
                + "join pg_class dst on dst.oid = con.confrelid "
                + "join pg_namespace dst_ns on dst_ns.oid = dst.relnamespace "
                + "where con.contype = 'f' and src_ns.nspname <> dst_ns.nspname "
                + "and ? in (src_ns.nspname, dst_ns.nspname)";
        try (Connection connection = dataSource.getConnection()) {
            List<String> drops = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, schema);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        drops.add("alter table " + quote(resultSet.getString(1)) + "." + quote(resultSet.getString(2))
                                + " drop constraint if exists " + quote(resultSet.getString(3)));
                    }
                }
            }
            if (drops.isEmpty()) {
                return;
            }
            executeInTransaction(connection, drops);
            LOGGER.warn("Dropped {} foreign keys crossing the {} schema boundary (CON-02)", drops.size(), schema);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not drop the foreign keys that cross the " + schema + " schema",
                    exception);
        }
    }

    private static Set<String> existingTables(Connection connection, String schema, List<String> tables)
            throws SQLException {
        String query = "select table_name from information_schema.tables "
                + "where table_schema = ? and table_type = 'BASE TABLE' and table_name = any (?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, schema);
            statement.setArray(2, connection.createArrayOf("text", tables.toArray()));
            Set<String> existing = new TreeSet<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    existing.add(resultSet.getString(1));
                }
            }
            return existing;
        }
    }

    private static void moveToSchema(Connection connection, String schema, List<String> tables) throws SQLException {
        List<String> statements = new ArrayList<>();
        statements.add("create schema if not exists " + schema);
        for (String table : tables) {
            statements.add("alter table " + LEGACY_SCHEMA + "." + table + " set schema " + schema);
        }
        executeInTransaction(connection, statements);
    }

    private static void executeInTransaction(Connection connection, List<String> statements) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private static void requireIdentifier(String identifier) {
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid schema or table name: " + identifier);
        }
    }
}

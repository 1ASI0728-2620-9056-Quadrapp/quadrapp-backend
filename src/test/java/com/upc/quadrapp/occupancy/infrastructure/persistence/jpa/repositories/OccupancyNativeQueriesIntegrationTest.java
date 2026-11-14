package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.OccupancySnapshotJpaEntity;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.OccupancySnapshotJpaRepository.OccupancyCountsProjection;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The occupancy repositories use native SQL, which Hibernate validation never checks.
 * These queries must run against the module schemas: occupancy for its own tables and
 * configuration for the parking lot layout.
 */
@QuadrappIntegrationTest
@Transactional
class OccupancyNativeQueriesIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-13T15:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OccupancySnapshotJpaRepository snapshotRepository;

    @Autowired
    private SensorEventJpaRepository sensorEventRepository;

    private Long parkingLotId;
    private Long zoneId;
    private Long levelId;

    @BeforeEach
    void insertParkingLotWithOneZoneLevelAndThreeSpaces() {
        parkingLotId = jdbcTemplate.queryForObject("""
                insert into configuration.parking_lots (accuracy_margin_pct, active, flow_window_minutes,
                    freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s,
                    saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name)
                values (10, true, 15, 30, 0, 20, 30, 90, 300, now(), 7, now(), 'Main lot')
                returning id
                """, Long.class);
        zoneId = jdbcTemplate.queryForObject("""
                insert into configuration.zones (sort_order, created_at, lot_id, updated_at, name)
                values (1, now(), ?, now(), 'Zone A') returning id
                """, Long.class, parkingLotId);
        levelId = jdbcTemplate.queryForObject("""
                insert into configuration.levels (sort_order, created_at, updated_at, zone_id, name)
                values (1, now(), now(), ?, 'Level 1') returning id
                """, Long.class, zoneId);
        insertSpace("A-1", "FREE");
        insertSpace("A-2", "OCCUPIED");
        insertSpace("A-3", "UNKNOWN");
    }

    @Test
    void summarizesTheSpacesOfEachScope() {
        assertCounts(snapshotRepository.summarize("PARKING_LOT", parkingLotId));
        assertCounts(snapshotRepository.summarize("ZONE", zoneId));
        assertCounts(snapshotRepository.summarize("LEVEL", levelId));
    }

    @Test
    void resolvesTheParkingLotOfZonesAndLevels() {
        assertThat(snapshotRepository.findParkingLotIdForZone(zoneId)).contains(parkingLotId);
        assertThat(snapshotRepository.findParkingLotIdForLevel(levelId)).contains(parkingLotId);
        assertThat(snapshotRepository.findZoneIdsForParkingLot(parkingLotId)).containsExactly(zoneId);
    }

    @Test
    void findsTheSnapshotsOfAParkingLot() {
        snapshotRepository.saveAndFlush(snapshot(NOW.minus(10, ChronoUnit.MINUTES)));
        snapshotRepository.saveAndFlush(snapshot(NOW));

        assertThat(snapshotRepository.findByParkingLotId(parkingLotId))
                .extracting(OccupancySnapshotJpaEntity::getTakenAt)
                .containsExactly(NOW.minus(10, ChronoUnit.MINUTES), NOW);
        assertThat(snapshotRepository.findByParkingLotIdAndTakenAtBetween(
                parkingLotId, NOW.minus(1, ChronoUnit.MINUTES), NOW))
                .extracting(OccupancySnapshotJpaEntity::getTakenAt)
                .containsExactly(NOW);
    }

    @Test
    void storesEachSensorEventOnlyOnce() {
        Long spaceId = jdbcTemplate.queryForObject(
                "select id from configuration.spaces where label = 'A-1'", Long.class);

        int first = sensorEventRepository.insertIfAbsent("evt-1", 11L, spaceId, null, "OCCUPIED", NOW, NOW);
        int replay = sensorEventRepository.insertIfAbsent("evt-1", 11L, spaceId, null, "OCCUPIED", NOW, NOW);

        assertThat(first).isEqualTo(1);
        assertThat(replay).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from occupancy.sensor_events where event_id = 'evt-1'", Integer.class))
                .isEqualTo(1);
    }

    private void assertCounts(OccupancyCountsProjection counts) {
        assertThat(counts.getTotalSpaces()).isEqualTo(3L);
        assertThat(counts.getFreeCount()).isEqualTo(1L);
        assertThat(counts.getOccupiedCount()).isEqualTo(1L);
        assertThat(counts.getUnknownCount()).isEqualTo(1L);
    }

    private void insertSpace(String label, String status) {
        jdbcTemplate.update("""
                insert into configuration.spaces (active, created_at, level_id, status_since, updated_at,
                    status, type, label, unknown_reason)
                values (true, now(), ?, now(), now(), ?, 'STANDARD', ?, ?)
                """, levelId, status, label, "UNKNOWN".equals(status) ? "NO_SENSOR" : null);
    }

    private OccupancySnapshotJpaEntity snapshot(Instant takenAt) {
        OccupancySnapshotJpaEntity snapshot = new OccupancySnapshotJpaEntity();
        snapshot.setZoneId(zoneId);
        snapshot.setTakenAt(takenAt);
        snapshot.setTotalSpaces((short) 3);
        snapshot.setFreeCount((short) 1);
        snapshot.setOccupiedCount((short) 1);
        snapshot.setUnknownCount((short) 1);
        return snapshot;
    }
}

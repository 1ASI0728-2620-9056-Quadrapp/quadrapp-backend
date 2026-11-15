package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceIncidentJpaEntity;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceJpaEntity;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Devices and incidents resolve their institution from the parking lot, which lives in the
 * {@code configuration} schema. Loading them must work now that every module has its own schema.
 */
@QuadrappIntegrationTest
@Transactional
class DeviceInstitutionFormulaIntegrationTest {

    private static final long INSTITUTION_ID = 7L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DeviceJpaRepository deviceRepository;

    @Autowired
    private DeviceIncidentJpaRepository incidentRepository;

    private Long parkingLotId;

    @BeforeEach
    void insertParkingLotWithDeviceAndIncident() {
        parkingLotId = jdbcTemplate.queryForObject("""
                insert into configuration.parking_lots (accuracy_margin_pct, active, flow_window_minutes,
                    freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s,
                    saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name)
                values (10, true, 15, 30, 0, 20, 30, 90, 300, now(), ?, now(), 'Main lot')
                returning id
                """, Long.class, INSTITUTION_ID);
        jdbcTemplate.update("""
                insert into sensing.devices (expected_report_interval_s, created_at, lot_id, updated_at,
                    status, type, hardware_id)
                values (60, now(), ?, now(), 'OPERATIONAL', 'SPACE_SENSOR', 'ESP32-FORMULA-TEST')
                """, parkingLotId);
        jdbcTemplate.update("""
                insert into sensing.device_incidents (detected_at, lot_id, type, hardware_id)
                values (now(), ?, 'USER_REPORT', 'ESP32-FORMULA-TEST')
                """, parkingLotId);
    }

    @Test
    void deviceResolvesItsInstitutionFromTheParkingLot() {
        List<DeviceJpaEntity> devices =
                deviceRepository.findByInstitutionIdAndParkingLotId(INSTITUTION_ID, parkingLotId);

        assertThat(devices)
                .singleElement()
                .satisfies(device -> {
                    assertThat(device.getHardwareId()).isEqualTo("ESP32-FORMULA-TEST");
                    assertThat(device.getInstitutionId()).isEqualTo(INSTITUTION_ID);
                });
    }

    @Test
    void incidentResolvesItsInstitutionFromTheParkingLot() {
        List<DeviceIncidentJpaEntity> incidents =
                incidentRepository.findByInstitutionIdOrderByOccurredAtDesc(INSTITUTION_ID);

        assertThat(incidents)
                .singleElement()
                .satisfies(incident -> assertThat(incident.getInstitutionId()).isEqualTo(INSTITUTION_ID));
    }
}

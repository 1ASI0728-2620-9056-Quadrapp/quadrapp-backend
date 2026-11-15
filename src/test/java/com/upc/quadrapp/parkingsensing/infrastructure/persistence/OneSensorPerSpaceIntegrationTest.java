package com.upc.quadrapp.parkingsensing.infrastructure.persistence;

import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "A space has at most one sensor" is also enforced by the database, so two administrators
 * assigning sensors to the same space at the same time cannot both succeed.
 */
@QuadrappIntegrationTest
class OneSensorPerSpaceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void aSecondSensorCannotBeAssignedToTheSameSpace() {
        long space = 900_000L + (long) (Math.random() * 99_999);
        insertSensor(space);

        assertThatThrownBy(() -> insertSensor(space))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_devices_space");
    }

    private void insertSensor(long spaceId) {
        jdbc.update("insert into sensing.devices (expected_report_interval_s, created_at, lot_id, space_id, "
                        + "updated_at, status, type, hardware_id) values (60, now(), 1, ?, now(), 'OPERATIONAL', "
                        + "'SPACE_SENSOR', ?)", spaceId, "HW-" + UUID.randomUUID());
    }
}

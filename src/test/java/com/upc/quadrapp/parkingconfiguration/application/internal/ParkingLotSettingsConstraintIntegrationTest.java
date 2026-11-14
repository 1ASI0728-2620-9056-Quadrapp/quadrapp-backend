package com.upc.quadrapp.parkingconfiguration.application.internal;

import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database keeps the same ranges as the domain, so a value that does not fit can never be
 * stored and break later reads of the parking lot.
 */
@QuadrappIntegrationTest
class ParkingLotSettingsConstraintIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void aDebounceOutsideItsRangeIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> jdbc.update("insert into configuration.parking_lots (accuracy_margin_pct, active, "
                + "flow_window_minutes, freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name) "
                + "values (10, true, 15, 32000, 1, 20, 10, 90, 300, now(), 1, now(), 'Out of range')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_parking_lots_freed_debounce");
    }
}

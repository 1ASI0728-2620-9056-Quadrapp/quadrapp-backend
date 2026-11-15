package com.upc.quadrapp.parkingsensing.application.internal.commandservices;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.configuration.ParkingConfigurationLookupPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.ReadingProcessingPort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.TrustedReadingSourcePort;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingKind;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * A reading that does not match the device assignment is rejected, and the incident that
 * records the rejection stays stored: rejecting the reading must not roll it back. Retries of
 * rejected readings do not open more incidents while one is still open.
 */
@QuadrappIntegrationTest
class SensorReadingIncidentIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @MockitoBean
    private TrustedReadingSourcePort trustedSources;

    @MockitoBean
    private ReadingProcessingPort processing;

    @MockitoBean
    private DevicePersistencePort devices;

    @MockitoBean
    private ParkingConfigurationLookupPort configuration;

    @Autowired
    private SensorReadingCommandService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void rejectedReadingKeepsItsUnmappedSensorIncident() {
        Device passSensor = Device.restore(999L, 1L, 2L, "HW-INCIDENT-1", DeviceType.PASS_SENSOR, 60,
                DeviceStatus.OPERATIONAL, null, null, null, null, 1L, null, Boolean.FALSE, null, null,
                null, null, NOW, NOW);
        when(devices.findByHardwareId(anyString())).thenReturn(Optional.of(passSensor));
        when(processing.processOnce(any(), any(), any(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        jdbc.update("insert into configuration.parking_lots (id, accuracy_margin_pct, active, flow_window_minutes, "
                + "freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name) "
                + "values (2, 5, true, 15, 10, 1, 20, 10, 90, 300, now(), 1, now(), 'Incident lot') "
                + "on conflict do nothing");
        jdbc.update("delete from sensing.device_incidents where device_id = 999");
        SensorReading spaceReading = new SensorReading("evt-incident-1", "HW-INCIDENT-1", ReadingKind.SPACE,
                SensorState.OCCUPIED, null, NOW, null, null, null);

        assertThatThrownBy(() -> service.handle(new ProcessSensorReadingCommand(1L, spaceReading)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reading not allowed for device assignment.");

        assertThat(jdbc.queryForList(
                "select type from sensing.device_incidents where device_id = 999", String.class))
                .containsExactly("UNMAPPED_SENSOR");
    }

    @Test
    void retriesOfRejectedReadingsKeepASingleOpenIncident() {
        Device passSensor = Device.restore(998L, 1L, 2L, "HW-INCIDENT-2", DeviceType.PASS_SENSOR, 60,
                DeviceStatus.OPERATIONAL, null, null, null, null, 1L, null, Boolean.FALSE, null, null,
                null, null, NOW, NOW);
        when(devices.findByHardwareId(anyString())).thenReturn(Optional.of(passSensor));
        when(processing.processOnce(any(), any(), any(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        jdbc.update("insert into configuration.parking_lots (id, accuracy_margin_pct, active, flow_window_minutes, "
                + "freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name) "
                + "values (2, 5, true, 15, 10, 1, 20, 10, 90, 300, now(), 1, now(), 'Incident lot') "
                + "on conflict do nothing");
        jdbc.update("delete from sensing.device_incidents where device_id = 998");

        for (int attempt = 1; attempt <= 3; attempt++) {
            SensorReading retry = new SensorReading("evt-retry-" + attempt, "HW-INCIDENT-2", ReadingKind.SPACE,
                    SensorState.OCCUPIED, null, NOW.plusSeconds(attempt), null, null, null);
            assertThatThrownBy(() -> service.handle(new ProcessSensorReadingCommand(1L, retry)))
                    .isInstanceOf(IllegalStateException.class);
        }

        assertThat(jdbc.queryForObject(
                "select count(*) from sensing.device_incidents where device_id = 998 and resolved_at is null",
                Integer.class)).isOne();
    }
}

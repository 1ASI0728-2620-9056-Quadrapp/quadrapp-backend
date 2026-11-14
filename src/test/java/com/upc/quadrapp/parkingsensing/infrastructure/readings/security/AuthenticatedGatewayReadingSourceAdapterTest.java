package com.upc.quadrapp.parkingsensing.infrastructure.readings.security;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceStatus;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingKind;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A gateway may only report its own sensors (D7a): a compromised gateway cannot send readings
 * of sensors that belong to another gateway.
 */
class AuthenticatedGatewayReadingSourceAdapterTest {

    private static final long INSTITUTION = 1L;
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final DevicePersistencePort devices = mock(DevicePersistencePort.class);
    private final AuthenticatedGatewayReadingSourceAdapter adapter = new AuthenticatedGatewayReadingSourceAdapter(devices);

    @BeforeEach
    void gatewaysAndSensors() {
        when(devices.findByHardwareId("GW-A")).thenReturn(Optional.of(device(10L, "GW-A", DeviceType.GATEWAY, null)));
        when(devices.findByHardwareId("GW-B")).thenReturn(Optional.of(device(20L, "GW-B", DeviceType.GATEWAY, null)));
        when(devices.findByHardwareId("SENSOR-A")).thenReturn(
                Optional.of(device(11L, "SENSOR-A", DeviceType.SPACE_SENSOR, 10L)));
        var principal = new VerifiedGatewayPrincipal(INSTITUTION, "GW-A");
        var authentication = new TestingAuthenticationToken(principal, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aGatewayCanReportItsOwnSensors() {
        assertThatCode(() -> adapter.requireTrustedSource(INSTITUTION, reading("SENSOR-A", "GW-A")))
                .doesNotThrowAnyException();
    }

    @Test
    void aGatewayCannotReportSensorsOfAnotherGateway() {
        when(devices.findByHardwareId("SENSOR-B")).thenReturn(
                Optional.of(device(21L, "SENSOR-B", DeviceType.SPACE_SENSOR, 20L)));

        assertThatThrownBy(() -> adapter.requireTrustedSource(INSTITUTION, reading("SENSOR-B", "GW-A")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Sensor is not linked to this gateway.");
    }

    @Test
    void aSensorWithoutGatewayCannotBeReported() {
        when(devices.findByHardwareId("SENSOR-X")).thenReturn(
                Optional.of(device(31L, "SENSOR-X", DeviceType.SPACE_SENSOR, null)));

        assertThatThrownBy(() -> adapter.requireTrustedSource(INSTITUTION, reading("SENSOR-X", "GW-A")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Sensor is not linked to this gateway.");
    }

    private static SensorReading reading(String hardwareId, String gatewayHardwareId) {
        return new SensorReading("evt-1", hardwareId, ReadingKind.SPACE, SensorState.OCCUPIED, null, NOW,
                null, null, gatewayHardwareId);
    }

    private static Device device(Long id, String hardwareId, DeviceType type, Long gatewayId) {
        return Device.restore(id, INSTITUTION, 2L, hardwareId, type, 60, DeviceStatus.OPERATIONAL, gatewayId,
                type == DeviceType.SPACE_SENSOR ? 5L : null, null, null, 1L, null, Boolean.FALSE, null, null,
                null, null, NOW, NOW);
    }
}

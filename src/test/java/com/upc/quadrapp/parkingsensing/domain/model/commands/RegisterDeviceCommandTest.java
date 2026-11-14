package com.upc.quadrapp.parkingsensing.domain.model.commands;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.DeviceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sensors report through their gateway (CON-15), and a reading is accepted only from the
 * sensor's own gateway, so a sensor must be linked to one when it is registered.
 */
class RegisterDeviceCommandTest {

    @Test
    void aSensorWithoutGatewayIsRejected() {
        assertThatThrownBy(() -> command(DeviceType.SPACE_SENSOR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sensors must be linked to a gateway.");
        assertThatThrownBy(() -> command(DeviceType.PASS_SENSOR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sensors must be linked to a gateway.");
    }

    @Test
    void aSensorWithGatewayAndAGatewayWithoutOneAreAccepted() {
        assertThatCode(() -> command(DeviceType.SPACE_SENSOR, 10L)).doesNotThrowAnyException();
        assertThatCode(() -> command(DeviceType.GATEWAY, null)).doesNotThrowAnyException();
    }

    private static RegisterDeviceCommand command(DeviceType type, Long gatewayId) {
        return new RegisterDeviceCommand(1L, 2L, "HW-1", type, 60, gatewayId, null, null, 3L);
    }
}

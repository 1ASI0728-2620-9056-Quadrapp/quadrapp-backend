package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.parkingconfiguration.domain.model.exceptions.ParkingConfigurationNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParkingConfigurationExceptionHandlerTest {

    private final ParkingConfigurationExceptionHandler handler = new ParkingConfigurationExceptionHandler();

    @Test
    void missingResourceIs404WithItsMessage() {
        var response = handler.notFound(new ParkingConfigurationNotFoundException("Zone not found."));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Zone not found.");
    }
}

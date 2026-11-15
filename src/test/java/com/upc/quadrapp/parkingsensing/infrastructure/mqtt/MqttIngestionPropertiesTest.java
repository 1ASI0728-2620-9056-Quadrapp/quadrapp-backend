package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MqttIngestionPropertiesTest {

    @Test
    void defaultsToTheSensingSharedGroupAndARandomCleanSession() {
        var properties = new MqttIngestionProperties(true, null, null, null, null, null, null);

        assertThat(properties.url()).isEqualTo("tcp://localhost:1883");
        assertThat(properties.readingsSubscription()).isEqualTo("$share/sensing/quadrapp/+/+/readings");
        assertThat(properties.retryBackoff()).isEqualTo(Duration.ofMillis(200));
        assertThat(properties.hasFixedClientId()).isFalse();
    }

    @Test
    void usesTheConfiguredGroupAndClientId() {
        var properties = new MqttIngestionProperties(true, "ssl://broker:8883", "backend", "secret",
                "quadrapp-backend-1", "other", Duration.ofSeconds(1));

        assertThat(properties.readingsSubscription()).isEqualTo("$share/other/quadrapp/+/+/readings");
        assertThat(properties.hasFixedClientId()).isTrue();
    }
}

package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Wires the MQTT ingestion of gateway readings when {@code quadrapp.mqtt.enabled=true}.
 * Without it the application starts without a broker and receives no readings.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "quadrapp.mqtt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MqttIngestionProperties.class)
public class MqttInboundConfiguration {

    /** Pause before the first retry of a failing reading; each further retry waits one more step. */
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(200);

    @Bean
    public SensorReadingTranslator sensorReadingTranslator() {
        return new SensorReadingTranslator();
    }

    @Bean
    public MqttInboundListener mqttInboundListener(
            SensorReadingTranslator translator, SensorReadingCommandService readings,
            RejectedReadingStorePort rejected, @Qualifier("parkingSensingClock") Clock clock) {
        return new MqttInboundListener(translator, readings, rejected, clock, RETRY_BACKOFF);
    }

    @Bean
    public MqttReadingSubscriber mqttReadingSubscriber(MqttIngestionProperties properties, MqttInboundListener listener) {
        return new MqttReadingSubscriber(properties, listener);
    }
}

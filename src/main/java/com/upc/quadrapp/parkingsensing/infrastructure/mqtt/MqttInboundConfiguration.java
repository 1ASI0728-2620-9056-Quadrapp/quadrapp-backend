package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the MQTT ingestion of gateway readings when {@code quadrapp.mqtt.enabled=true}.
 * Without it the application starts without a broker and receives no readings.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "quadrapp.mqtt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MqttIngestionProperties.class)
public class MqttInboundConfiguration {

    @Bean
    public SensorReadingTranslator sensorReadingTranslator() {
        return new SensorReadingTranslator();
    }

    @Bean
    public MqttInboundListener mqttInboundListener(
            SensorReadingTranslator translator, SensorReadingCommandService readings,
            RejectedReadingStorePort rejected, @Qualifier("parkingSensingClock") Clock clock,
            MqttIngestionProperties properties) {
        return new MqttInboundListener(translator, readings, rejected, clock, properties.retryBackoff());
    }

    @Bean
    public MqttReadingSubscriber mqttReadingSubscriber(MqttIngestionProperties properties, MqttInboundListener listener) {
        return new MqttReadingSubscriber(properties, listener);
    }
}

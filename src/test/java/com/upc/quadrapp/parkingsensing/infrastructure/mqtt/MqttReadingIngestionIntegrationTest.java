package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingOutcome;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import com.upc.quadrapp.parkingsensing.infrastructure.readings.security.VerifiedGatewayPrincipal;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Publishes gateway messages to a real Mosquitto broker (MQTT 5) and checks that the backend,
 * subscribed through its shared subscription, processes or rejects them.
 */
@QuadrappIntegrationTest
class MqttReadingIngestionIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(15);
    private static final MqttTestBroker BROKER = new MqttTestBroker();

    @DynamicPropertySource
    static void mqtt(DynamicPropertyRegistry registry) {
        registry.add("quadrapp.mqtt.enabled", () -> "true");
        registry.add("quadrapp.mqtt.url", BROKER::url);
    }

    @MockitoBean
    private SensorReadingCommandService readings;

    @Autowired
    private ObjectProvider<MqttReadingSubscriber> subscriber;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void waitForTheSubscription() {
        MqttTestBroker.awaitTrue(() -> subscriber.getObject().isSubscribed(), WAIT, "backend subscribed");
    }

    @Test
    void processesAPublishedReadingAsTheGatewayOfItsTopic() throws Exception {
        List<Object> principals = new CopyOnWriteArrayList<>();
        when(readings.handle(any())).thenAnswer(invocation -> {
            principals.add(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
        });
        String eventId = UUID.randomUUID().toString();

        BROKER.publish("quadrapp/7/GW-IT/readings", """
                {"eventId":"%s","sensorId":"SN-IT-1","kind":"SPACE","state":"OCCUPIED",
                 "ts":"2026-11-14T12:00:00Z","battery":90,"rssi":-60}""".formatted(eventId));

        verify(readings, timeout(WAIT.toMillis())).handle(argThat((ProcessSensorReadingCommand command) ->
                command.institutionId() == 7L
                        && command.reading().eventId().equals(eventId)
                        && command.reading().hardwareId().equals("SN-IT-1")
                        && "GW-IT".equals(command.reading().gatewayHardwareId())));
        assertThat(principals).containsExactly(new VerifiedGatewayPrincipal(7L, "GW-IT"));
    }

    @Test
    void storesAMalformedPublishedMessageAsRejected() throws Exception {
        String topic = "quadrapp/7/GW-IT-BAD/readings";

        BROKER.publish(topic, "{\"kind\":\"DOOR\"}");

        MqttTestBroker.awaitTrue(() -> jdbc.queryForObject(
                "select count(*) from sensing.rejected_readings where topic = ?", Integer.class, topic) == 1,
                WAIT, "rejected message stored");
        assertThat(jdbc.queryForObject("select reason from sensing.rejected_readings where topic = ?",
                String.class, topic)).startsWith("Malformed message: ");
    }
}

package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingOutcome;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import com.upc.quadrapp.parkingsensing.infrastructure.readings.security.VerifiedGatewayPrincipal;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Publishes gateway messages to a real Mosquitto broker (MQTT 5) and checks that the backend,
 * subscribed through its shared subscription, processes or rejects them.
 */
@QuadrappIntegrationTest
class MqttReadingIngestionIntegrationTest {

    private static final long WAIT_MILLIS = 15_000;

    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2"))
            .withCopyToContainer(Transferable.of("listener 1883\nallow_anonymous true\n"),
                    "/mosquitto/config/mosquitto.conf")
            .withExposedPorts(1883)
            .waitingFor(Wait.forListeningPort());

    static {
        MOSQUITTO.start();
    }

    @DynamicPropertySource
    static void mqtt(DynamicPropertyRegistry registry) {
        registry.add("quadrapp.mqtt.enabled", () -> "true");
        registry.add("quadrapp.mqtt.url", MqttReadingIngestionIntegrationTest::brokerUrl);
    }

    @MockitoBean
    private SensorReadingCommandService readings;

    @Autowired
    private ObjectProvider<MqttReadingSubscriber> subscriber;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void waitForTheSubscription() {
        awaitTrue(() -> subscriber.getObject().isSubscribed(), "backend subscribed to the readings");
    }

    @Test
    void processesAPublishedReadingAsTheGatewayOfItsTopic() throws Exception {
        List<Object> principals = new CopyOnWriteArrayList<>();
        when(readings.handle(any())).thenAnswer(invocation -> {
            principals.add(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
        });
        String eventId = UUID.randomUUID().toString();

        publish("quadrapp/7/GW-IT/readings", """
                {"eventId":"%s","sensorId":"SN-IT-1","kind":"SPACE","state":"OCCUPIED",
                 "ts":"2026-11-14T12:00:00Z","battery":90,"rssi":-60}""".formatted(eventId));

        verify(readings, timeout(WAIT_MILLIS)).handle(org.mockito.ArgumentMatchers.<ProcessSensorReadingCommand>argThat(
                command -> command.institutionId() == 7L
                        && command.reading().eventId().equals(eventId)
                        && command.reading().hardwareId().equals("SN-IT-1")
                        && "GW-IT".equals(command.reading().gatewayHardwareId())));
        assertThat(principals).containsExactly(new VerifiedGatewayPrincipal(7L, "GW-IT"));
    }

    @Test
    void storesAMalformedPublishedMessageAsRejected() throws Exception {
        String topic = "quadrapp/7/GW-IT-BAD/readings";

        publish(topic, "{\"kind\":\"DOOR\"}");

        awaitTrue(() -> jdbc.queryForObject("select count(*) from sensing.rejected_readings where topic = ?",
                Integer.class, topic) == 1, "rejected message stored");
        assertThat(jdbc.queryForObject("select reason from sensing.rejected_readings where topic = ?",
                String.class, topic)).startsWith("Malformed message: ");
    }

    private static void publish(String topic, String payload) throws Exception {
        var publisher = new MqttClient(brokerUrl(), "test-gateway-" + UUID.randomUUID(), new MemoryPersistence());
        try {
            publisher.connect();
            publisher.publish(topic, payload.getBytes(StandardCharsets.UTF_8), MqttReadingSubscriber.QOS, false);
            publisher.disconnect();
        } finally {
            publisher.close();
        }
    }

    private static String brokerUrl() {
        return "tcp://" + MOSQUITTO.getHost() + ":" + MOSQUITTO.getMappedPort(1883);
    }

    private static void awaitTrue(BooleanSupplier condition, String description) {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out waiting for: " + description);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted waiting for: " + description, interrupted);
            }
        }
    }
}

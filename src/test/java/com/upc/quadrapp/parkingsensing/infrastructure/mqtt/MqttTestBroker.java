package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * A real Eclipse Mosquitto broker (MQTT 5, anonymous) for the ingestion tests. Each test class
 * starts its own broker: Spring keeps every test context alive, and two contexts subscribed to
 * one broker would share its readings.
 */
final class MqttTestBroker {

    private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2"))
            .withCopyToContainer(Transferable.of("listener 1883\nallow_anonymous true\n"),
                    "/mosquitto/config/mosquitto.conf")
            .withExposedPorts(1883)
            .waitingFor(Wait.forListeningPort());

    MqttTestBroker() {
        container.start();
    }

    String url() {
        return "tcp://" + container.getHost() + ":" + container.getMappedPort(1883);
    }

    /** Publishes as a gateway with QoS 1. */
    void publish(String topic, String payload) throws Exception {
        var publisher = new MqttClient(url(), "test-gateway-" + UUID.randomUUID(), new MemoryPersistence());
        try {
            publisher.connect();
            publisher.publish(topic, payload.getBytes(StandardCharsets.UTF_8), MqttReadingSubscriber.QOS, false);
            publisher.disconnect();
        } finally {
            publisher.close();
        }
    }

    static void awaitTrue(BooleanSupplier condition, Duration timeout, String description) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
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

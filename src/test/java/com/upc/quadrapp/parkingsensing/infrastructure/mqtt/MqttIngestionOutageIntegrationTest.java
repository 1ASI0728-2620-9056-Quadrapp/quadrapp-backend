package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingOutcome;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * A database outage makes every reading fail and every rejection fail to be stored. Mosquitto
 * stops delivering once 20 messages are unacknowledged and does not redeliver them on the same
 * connection, so the backend must not leave them unacknowledged: it keeps trying the current
 * reading and, once the database is back, every reading published before and after the outage
 * is processed without restarting anything.
 */
@QuadrappIntegrationTest
class MqttIngestionOutageIntegrationTest {

    private static final MqttTestBroker BROKER = new MqttTestBroker();
    private static final int READINGS_DURING_OUTAGE = 30;

    @DynamicPropertySource
    static void mqtt(DynamicPropertyRegistry registry) {
        registry.add("quadrapp.mqtt.enabled", () -> "true");
        registry.add("quadrapp.mqtt.url", BROKER::url);
        registry.add("quadrapp.mqtt.retry-backoff", () -> "10ms");
    }

    @MockitoBean
    private SensorReadingCommandService readings;

    @MockitoBean
    private RejectedReadingStorePort rejected;

    @Autowired
    private ObjectProvider<MqttReadingSubscriber> subscriber;

    private volatile boolean databaseDown = true;
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Test
    void resumesDeliveryOnItsOwnAfterADatabaseOutage() throws Exception {
        when(readings.handle(any())).thenAnswer(invocation -> {
            if (databaseDown) {
                throw new DataAccessResourceFailureException("database down");
            }
            processed.add(invocation.<ProcessSensorReadingCommand>getArgument(0).reading().eventId());
            return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
        });
        doAnswer(invocation -> {
            if (databaseDown) {
                throw new DataAccessResourceFailureException("database down");
            }
            return null;
        }).when(rejected).store(any(), any(), any(), any());
        MqttTestBroker.awaitTrue(() -> subscriber.getObject().isSubscribed(), Duration.ofSeconds(15),
                "backend subscribed");

        List<String> published = new ArrayList<>();
        for (int i = 0; i < READINGS_DURING_OUTAGE; i++) {
            published.add(publishReading());
        }
        Thread.sleep(3_000);
        databaseDown = false;
        published.add(publishReading());

        MqttTestBroker.awaitTrue(() -> processed.containsAll(published), Duration.ofSeconds(30),
                "every reading processed after the outage");
        assertThat(processed).containsExactlyInAnyOrderElementsOf(published);
        assertThat(subscriber.getObject().isSubscribed()).isTrue();
    }

    private String publishReading() throws Exception {
        String eventId = UUID.randomUUID().toString();
        BROKER.publish("quadrapp/7/GW-OUTAGE/readings", """
                {"eventId":"%s","sensorId":"SN-OUTAGE","kind":"HEARTBEAT","ts":"2026-11-14T12:00:00Z"}"""
                .formatted(eventId));
        return eventId;
    }
}

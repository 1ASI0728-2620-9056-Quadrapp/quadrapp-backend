package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jdbc;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Stores rejected gateway messages in {@code sensing.rejected_readings}.
 */
@Component
public class JdbcRejectedReadingStore implements RejectedReadingStorePort {

    private final JdbcClient jdbc;

    public JdbcRejectedReadingStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void store(String topic, String payload, String reason, Instant receivedAt) {
        jdbc.sql("insert into sensing.rejected_readings (topic, payload, reason, received_at) "
                        + "values (:topic, :payload, :reason, :receivedAt)")
                .param("topic", topic == null ? "" : topic)
                .param("payload", payload == null ? "" : payload)
                .param("reason", reason)
                .param("receivedAt", Timestamp.from(receivedAt))
                .update();
    }
}

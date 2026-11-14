package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jdbc;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Stores rejected gateway messages in {@code sensing.rejected_readings}.
 * <p>
 * Any text a gateway sends must be storable, or the message could never be settled: PostgreSQL
 * text rejects U+0000, so NUL characters are stored escaped as {@code \\u0000}, and long payloads
 * are cut to {@value #MAX_PAYLOAD_CHARS} characters.
 */
@Component
public class JdbcRejectedReadingStore implements RejectedReadingStorePort {

    static final int MAX_PAYLOAD_CHARS = 65_536;

    private final JdbcClient jdbc;

    public JdbcRejectedReadingStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void store(String topic, String payload, String reason, Instant receivedAt) {
        jdbc.sql("insert into sensing.rejected_readings (topic, payload, reason, received_at) "
                        + "values (:topic, :payload, :reason, :receivedAt)")
                .param("topic", storable(topic))
                .param("payload", truncated(storable(payload)))
                .param("reason", storable(reason))
                .param("receivedAt", Timestamp.from(receivedAt))
                .update();
    }

    private static String storable(String text) {
        return text == null ? "" : text.replace("\u0000", "\\u0000");
    }

    private static String truncated(String text) {
        return text.length() <= MAX_PAYLOAD_CHARS ? text : text.substring(0, MAX_PAYLOAD_CHARS);
    }
}

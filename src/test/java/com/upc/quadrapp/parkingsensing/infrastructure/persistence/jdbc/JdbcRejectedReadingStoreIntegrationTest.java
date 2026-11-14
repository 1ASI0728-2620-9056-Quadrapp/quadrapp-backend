package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jdbc;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@QuadrappIntegrationTest
class JdbcRejectedReadingStoreIntegrationTest {

    @Autowired
    private RejectedReadingStorePort store;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void storesTheRejectedMessageAsReceived() {
        Instant receivedAt = Instant.parse("2026-11-14T12:00:00Z");

        store.store("quadrapp/7/GW-STORE/readings", "{\"kind\":\"DOOR\"}", "Malformed message: kind", receivedAt);

        var row = jdbc.queryForMap("select topic, payload, reason, received_at from sensing.rejected_readings "
                + "where topic = 'quadrapp/7/GW-STORE/readings'");
        assertThat(row.get("payload")).isEqualTo("{\"kind\":\"DOOR\"}");
        assertThat(row.get("reason")).isEqualTo("Malformed message: kind");
        assertThat(((Timestamp) row.get("received_at")).toInstant()).isEqualTo(receivedAt);
    }
}

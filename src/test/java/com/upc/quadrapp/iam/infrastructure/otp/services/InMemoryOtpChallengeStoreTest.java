package com.upc.quadrapp.iam.infrastructure.otp.services;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryOtpChallengeStoreTest {

    private static final String EMAIL = "driver@upc.edu.pe";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final InMemoryOtpChallengeStore store = new InMemoryOtpChallengeStore();

    @Test
    void exhaustedChallengeRejectsTheRightCodeAndKeepsTheCooldown() {
        store.issue(EMAIL, "right", NOW.plusSeconds(300), NOW);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(store.consume(EMAIL, "wrong", NOW.plusSeconds(1))).isFalse();
        }

        assertThat(store.consume(EMAIL, "right", NOW.plusSeconds(2))).isFalse();
        assertThat(store.issue(EMAIL, "next", NOW.plusSeconds(302), NOW.plusSeconds(2))).isFalse();
        assertThat(store.issue(EMAIL, "next", NOW.plusSeconds(361), NOW.plusSeconds(61))).isTrue();
        assertThat(store.consume(EMAIL, "next", NOW.plusSeconds(62))).isTrue();
    }

    @Test
    void requestsAboveTheLimitOfTheWindowAreRefused() {
        Duration window = Duration.ofMinutes(15);
        for (int request = 0; request < 5; request++) {
            assertThat(store.registerRequest(EMAIL, 5, window, NOW.plusSeconds(request))).isTrue();
        }

        assertThat(store.registerRequest(EMAIL, 5, window, NOW.plusSeconds(10))).isFalse();
        assertThat(store.registerRequest("other@upc.edu.pe", 5, window, NOW.plusSeconds(10))).isTrue();
        assertThat(store.registerRequest(EMAIL, 5, window, NOW.plus(window).plusSeconds(1))).isTrue();
    }
}

package com.upc.quadrapp.prediction.infrastructure.realtime;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.upc.quadrapp.prediction.PredictionTestSupport.occupy;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * After each generation run, the subscribers of {@code /topic/parking-lots/{lotId}/forecasts}
 * receive the new forecasts of that parking lot; another institution's lot cannot be subscribed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "quadrapp.security.jwt.secret-base64="
                + "cXVhZHJhcHAtaW50ZWdyYXRpb24tdGVzdC1qd3Qtc2lnbmluZy1rZXktMDEyMzQ1Njc4OQ==",
        "quadrapp.security.hash-pepper-base64="
                + "cXVhZHJhcHAtaW50ZWdyYXRpb24tdGVzdC1oYXNoLXBlcHBlci0wMTIzNDU2Nzg5",
        "quadrapp.scheduling.enabled=false",
        "quadrapp.events.retry.min-age=PT0S"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class ForecastRealtimeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private OccupancyCommandService occupancy;

    @Autowired
    private ForecastCommandService forecasts;

    @Autowired
    private SimpUserRegistry userRegistry;

    private WebSocketStompClient client;
    private OccupancyTestLayout own;
    private OccupancyTestLayout other;

    @BeforeEach
    void setUp() {
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MessageConverter() {
            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass) {
                return new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
            }

            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers) {
                return MessageBuilder.createMessage(payload.toString().getBytes(StandardCharsets.UTF_8), headers);
            }
        });
        own = OccupancyTestLayout.create(jdbc, 2, 10);
        other = OccupancyTestLayout.create(jdbc, 2, 10);
    }

    @AfterEach
    void tearDown() {
        client.stop();
    }

    @Test
    void aSubscriberReceivesTheForecastsOfEachRunOfItsParkingLot() throws Exception {
        occupy(occupancy, own, 0);
        StompSession session = connect(token(own.userId)).join();
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        String topic = "/topic/parking-lots/" + own.parkingLotId + "/forecasts";
        session.subscribe(topic, collecting(messages));
        awaitSubscription(topic);

        forecasts.generateScheduledRuns();

        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull()
                .contains("\"parkingLotId\":" + own.parkingLotId)
                .contains("\"trigger\":\"SCHEDULED\"")
                .contains("\"runId\":")
                .contains("\"horizonMinutes\":15")
                .contains("\"horizonMinutes\":60")
                .contains("\"modelVersion\":\"persistence-v1\"");
    }

    @Test
    void aSubscriptionToAnotherInstitutionsForecastsIsRefused() throws Exception {
        CompletableFuture<String> errors = new CompletableFuture<>();
        StompSession session = connect(token(own.userId), errors).join();

        session.subscribe("/topic/parking-lots/" + other.parkingLotId + "/forecasts",
                collecting(new LinkedBlockingQueue<>()));

        assertThat(errors.get(5, TimeUnit.SECONDS)).contains("Parking lot not found.");
    }

    private CompletableFuture<StompSession> connect(String token) {
        return connect(token, new CompletableFuture<>());
    }

    private CompletableFuture<StompSession> connect(String token, CompletableFuture<String> errors) {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
        return client.connectAsync("ws://localhost:" + port + "/ws", (WebSocketHttpHeaders) null, headers,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(StompHeaders frameHeaders, Object payload) {
                        errors.complete("ERROR " + frameHeaders.getFirst("message"));
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        errors.complete(exception.toString());
                    }
                });
    }

    private void awaitSubscription(String topic) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (userRegistry.findSubscriptions(subscription -> topic.equals(subscription.getDestination())).isEmpty()) {
            assertThat(System.currentTimeMillis()).as("subscription registered").isLessThan(deadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
    }

    private static StompFrameHandler collecting(BlockingQueue<String> messages) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((String) payload);
            }
        };
    }

    private String token(long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("quadrapp")
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("role", "OPERATOR")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}

package com.upc.quadrapp.occupancy.infrastructure.realtime;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The app and the console subscribe to {@code /topic/parking-lots/{lotId}/occupancy} over STOMP
 * with the same access token as the REST API, and receive the changes of their own parking lots
 * only.
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
class OccupancyRealtimeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private OccupancyCommandService commands;

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
    void aSubscriberReceivesTheChangesOfItsParkingLot() throws Exception {
        Session session = connect(token(own.userId));
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        subscribe(session, own.parkingLotId, messages);

        commands.handle(new RecordSensorEventCommand(own.institutionId, own.parkingLotId, 1L, own.space(0), null,
                UUID.randomUUID().toString(), OccupancyEventType.OCCUPIED, Instant.parse("2026-11-14T10:00:00Z")));

        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull()
                .contains("\"parkingLotId\":" + own.parkingLotId)
                .contains("\"zoneId\":" + own.zoneId)
                .contains("\"occupiedCount\":1")
                .contains("\"spaceId\":" + own.space(0))
                .contains("\"status\":\"OCCUPIED\"");
    }

    @Test
    void aSubscriptionToAnotherInstitutionsParkingLotIsRefused() throws Exception {
        Session session = connect(token(own.userId));

        session.stomp().subscribe("/topic/parking-lots/" + other.parkingLotId + "/occupancy", ignoring());

        assertThat(session.errors().get(5, TimeUnit.SECONDS)).contains("Parking lot not found.");
    }

    @Test
    void aConnectionWithoutAValidTokenIsRefused() throws Exception {
        Session withoutToken = connect(null);
        Session withForgedToken = connect("not-a-token");

        assertThat(withoutToken.errors().get(5, TimeUnit.SECONDS)).contains("A bearer access token is required.");
        assertThat(withForgedToken.errors().get(5, TimeUnit.SECONDS)).contains("The access token is not valid.");
    }

    private record Session(StompSession stomp, CompletableFuture<String> errors) {}

    private Session connect(String token) throws Exception {
        CompletableFuture<String> errors = new CompletableFuture<>();
        CompletableFuture<StompSession> connected = new CompletableFuture<>();
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
        client.connectAsync("ws://localhost:" + port + "/ws", (WebSocketHttpHeaders) null, headers, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connected.complete(session);
            }

            @Override
            public void handleFrame(StompHeaders frameHeaders, Object payload) {
                errors.complete("ERROR " + frameHeaders.getFirst("message"));
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders frameHeaders,
                                        byte[] payload, Throwable exception) {
                errors.complete(exception.toString());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                errors.complete(exception.toString());
            }
        });
        CompletableFuture.anyOf(connected, errors).get(5, TimeUnit.SECONDS);
        return new Session(connected.getNow(null), errors);
    }

    /** Subscribes and waits until the broker registered the subscription. */
    private void subscribe(Session session, Long parkingLotId, BlockingQueue<String> messages) {
        String topic = "/topic/parking-lots/" + parkingLotId + "/occupancy";
        session.stomp().subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((String) payload);
            }
        });
        long deadline = System.currentTimeMillis() + 5_000;
        while (userRegistry.findSubscriptions(subscription -> topic.equals(subscription.getDestination())).isEmpty()) {
            assertThat(System.currentTimeMillis()).as("subscription registered").isLessThan(deadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
    }

    private static StompFrameHandler ignoring() {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
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

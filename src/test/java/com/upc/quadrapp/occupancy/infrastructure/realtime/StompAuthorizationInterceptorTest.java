package com.upc.quadrapp.occupancy.infrastructure.realtime;

import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The STOMP 1.2 {@code STOMP} frame opens a session exactly like CONNECT, and a session whose
 * access token expired cannot subscribe any more.
 */
class StompAuthorizationInterceptorTest {

    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final OccupancyQueryService queries = mock(OccupancyQueryService.class);
    private final StompAuthorizationInterceptor interceptor = new StompAuthorizationInterceptor(decoder, queries);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void theStompFrameIsAuthenticatedLikeConnect() {
        when(decoder.decode("token")).thenReturn(jwt(Instant.now().plusSeconds(300)));
        StompHeaderAccessor accessor = accessor(StompCommand.STOMP);
        accessor.setNativeHeader("Authorization", "Bearer token");

        interceptor.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isInstanceOf(JwtAuthenticationToken.class);
    }

    @Test
    void theStompFrameWithoutATokenIsRefused() {
        StompHeaderAccessor accessor = accessor(StompCommand.STOMP);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("A bearer access token is required.");
    }

    @Test
    void aSubscriptionWithAnExpiredTokenIsRefused() {
        when(queries.isReadableParkingLot(7L)).thenReturn(true);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/parking-lots/7/occupancy");
        accessor.setUser(new JwtAuthenticationToken(jwt(Instant.now().minusSeconds(1)), List.of()));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("The access token has expired.");
    }

    @Test
    void aSubscriptionWithAValidTokenIsAllowed() {
        when(queries.isReadableParkingLot(7L)).thenReturn(true);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/parking-lots/7/occupancy");
        accessor.setUser(new JwtAuthenticationToken(jwt(Instant.now().plusSeconds(300)), List.of()));

        Message<?> message = message(accessor);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    private static StompHeaderAccessor accessor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Jwt jwt(Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("42")
                .claim("role", "DRIVER")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .build();
    }
}

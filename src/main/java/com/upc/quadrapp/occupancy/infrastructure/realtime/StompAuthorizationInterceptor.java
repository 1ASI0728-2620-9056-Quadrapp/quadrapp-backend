package com.upc.quadrapp.occupancy.infrastructure.realtime;

import com.upc.quadrapp.occupancy.domain.services.OccupancyQueryService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorizes the STOMP channel with the same access token as the REST API.
 * <ul>
 *     <li>CONNECT must carry {@code Authorization: Bearer <access token>}; the token is validated
 *     with the same decoder as the resource server and becomes the user of the session.</li>
 *     <li>SUBSCRIBE is allowed only to {@code /topic/parking-lots/{lotId}/occupancy} of a parking
 *     lot the user can read; another institution's lot is refused like a missing one.</li>
 *     <li>Clients cannot SEND: the channel only delivers changes.</li>
 * </ul>
 * A refused frame closes the session with a STOMP ERROR frame.
 */
@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern OCCUPANCY_TOPIC = Pattern.compile("^/topic/parking-lots/(\\d{1,18})/occupancy$");
    private static final String BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final OccupancyQueryService queries;

    public StompAuthorizationInterceptor(JwtDecoder jwtDecoder, OccupancyQueryService queries) {
        this.jwtDecoder = jwtDecoder;
        this.queries = queries;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        } else if (command == StompCommand.SEND) {
            throw new AccessDeniedException("Clients cannot send messages on this channel.");
        }
        return message;
    }

    private JwtAuthenticationToken authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new AccessDeniedException("A bearer access token is required.");
        }
        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER.length()).trim());
            String role = jwt.getClaimAsString("role");
            return new JwtAuthenticationToken(
                    jwt,
                    role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        } catch (JwtException exception) {
            throw new AccessDeniedException("The access token is not valid.", exception);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof JwtAuthenticationToken user)) {
            throw new AccessDeniedException("The session is not authenticated.");
        }
        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : OCCUPANCY_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new AccessDeniedException("Unknown destination.");
        }
        Long parkingLotId = Long.valueOf(matcher.group(1));
        if (!readableAs(user, parkingLotId)) {
            throw new AccessDeniedException("Parking lot not found.");
        }
    }

    /** Checks the parking lot as the session user, the same way the REST queries do. */
    private boolean readableAs(JwtAuthenticationToken user, Long parkingLotId) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(user);
        SecurityContextHolder.setContext(context);
        try {
            return queries.isReadableParkingLot(parkingLotId);
        } catch (AccessDeniedException exception) {
            return false;
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}

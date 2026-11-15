package com.upc.quadrapp.occupancy.infrastructure.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * STOMP over WebSocket at {@code /ws}. The app and the console subscribe to
 * {@code /topic/parking-lots/{lotId}/occupancy}; {@link StompAuthorizationInterceptor} checks the
 * access token on CONNECT and the parking lot on every SUBSCRIBE.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
public class StompRealtimeConfiguration implements WebSocketMessageBrokerConfigurer {

    private final StompAuthorizationInterceptor authorization;
    private final String[] allowedOrigins;

    public StompRealtimeConfiguration(
            StompAuthorizationInterceptor authorization,
            @Value("${quadrapp.realtime.allowed-origins:*}") String[] allowedOrigins
    ) {
        this.authorization = authorization;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
        registry.setErrorHandler(new StompSubProtocolErrorHandler() {
            /** The ERROR frame names why the frame was refused instead of a generic delivery failure. */
            @Override
            public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
                Throwable cause = ex.getCause() instanceof AccessDeniedException ? ex.getCause() : ex;
                return super.handleClientMessageProcessingError(clientMessage, cause);
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorization);
    }
}

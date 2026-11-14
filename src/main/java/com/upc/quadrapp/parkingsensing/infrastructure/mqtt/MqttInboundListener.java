package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import com.upc.quadrapp.parkingsensing.infrastructure.readings.security.VerifiedGatewayPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Receives the gateway readings published on {@code quadrapp/{tenantId}/{gatewayId}/readings}.
 * <p>
 * The broker only lets each gateway publish under its own institution and gateway prefix, so the
 * topic identifies the sender: the reading is processed as a {@link VerifiedGatewayPrincipal}
 * of that institution and gateway. The message is acknowledged only after it was processed, so a
 * crash before that makes the broker deliver it again; duplicates are dropped by {@code eventId}.
 * <p>
 * A malformed message is stored as rejected and acknowledged. So is a reading whose topic does
 * not match the device (another institution, or a gateway that may not report it): it is
 * discarded without retrying. Any other processing failure is retried up
 * to {@value #MAX_RETRIES} times; if it still fails, the message is stored as rejected and
 * acknowledged so it does not block the next ones. Incidents for rejected readings are registered
 * by the sensing service itself. If the rejected message cannot be stored, it is not acknowledged.
 */
public class MqttInboundListener {

    static final int MAX_RETRIES = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttInboundListener.class);

    private final SensorReadingTranslator translator;
    private final SensorReadingCommandService readings;
    private final RejectedReadingStorePort rejected;
    private final Clock clock;
    private final Duration retryBackoff;

    public MqttInboundListener(SensorReadingTranslator translator, SensorReadingCommandService readings,
                               RejectedReadingStorePort rejected, Clock clock, Duration retryBackoff) {
        this.translator = Objects.requireNonNull(translator);
        this.readings = Objects.requireNonNull(readings);
        this.rejected = Objects.requireNonNull(rejected);
        this.clock = Objects.requireNonNull(clock);
        this.retryBackoff = Objects.requireNonNull(retryBackoff);
    }

    /**
     * Processes one message and runs {@code acknowledge} once it may be confirmed to the broker.
     * An exception means the message was neither processed nor stored, and must not be confirmed.
     */
    public void onReading(String topic, byte[] payload, Runnable acknowledge) {
        Instant receivedAt = clock.instant();
        String text = payload == null ? "" : new String(payload, StandardCharsets.UTF_8);

        GatewayReading gatewayReading;
        try {
            gatewayReading = translator.translate(topic, payload);
        } catch (MalformedReadingException malformed) {
            LOGGER.warn("Rejected malformed reading on {}: {}", topic, malformed.getMessage());
            rejected.store(topic, text, "Malformed message: " + malformed.getMessage(), receivedAt);
            acknowledge.run();
            return;
        }

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 1 + MAX_RETRIES; attempt++) {
            if (attempt > 1) {
                pauseBeforeRetry(attempt - 1);
            }
            try {
                process(gatewayReading);
                lastFailure = null;
                break;
            } catch (AccessDeniedException | SecurityException mismatch) {
                LOGGER.warn("Discarded reading {} on {}: {}", gatewayReading.reading().eventId(), topic,
                        mismatch.getMessage());
                rejected.store(topic, text, "Source mismatch: " + describe(mismatch), receivedAt);
                acknowledge.run();
                return;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                LOGGER.warn("Reading {} on {} failed (attempt {} of {}): {}", gatewayReading.reading().eventId(),
                        topic, attempt, 1 + MAX_RETRIES, failure.toString());
            }
        }
        if (lastFailure != null) {
            rejected.store(topic, text, "Processing failed after " + (1 + MAX_RETRIES) + " attempts: "
                    + describe(lastFailure), receivedAt);
        }
        acknowledge.run();
    }

    private void process(GatewayReading gatewayReading) {
        var principal = new VerifiedGatewayPrincipal(gatewayReading.institutionId(),
                gatewayReading.reading().gatewayHardwareId());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        try {
            readings.handle(new ProcessSensorReadingCommand(gatewayReading.institutionId(), gatewayReading.reading()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String describe(RuntimeException failure) {
        return failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }

    private void pauseBeforeRetry(int retry) {
        if (retryBackoff.isZero()) {
            return;
        }
        try {
            Thread.sleep(retryBackoff.multipliedBy(retry));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying a reading.", interrupted);
        }
    }
}

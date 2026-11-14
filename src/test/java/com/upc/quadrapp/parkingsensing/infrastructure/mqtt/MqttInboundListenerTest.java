package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.RejectedReadingStorePort;
import com.upc.quadrapp.parkingsensing.domain.model.commands.ProcessSensorReadingCommand;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingKind;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingOutcome;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import com.upc.quadrapp.parkingsensing.domain.services.SensorReadingCommandService;
import com.upc.quadrapp.parkingsensing.infrastructure.readings.security.VerifiedGatewayPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MqttInboundListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final String TOPIC = "quadrapp/7/GW-01/readings";
    private static final String VALID = """
            {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"SPACE",
             "state":"OCCUPIED","readingAt":"2026-11-14T11:59:58Z","battery":80}""";

    private final SensorReadingCommandService readings = mock(SensorReadingCommandService.class);
    private final RejectedReadingStorePort rejected = mock(RejectedReadingStorePort.class);
    private final Runnable acknowledge = mock(Runnable.class);
    private final MqttInboundListener listener = new MqttInboundListener(new SensorReadingTranslator(), readings,
            rejected, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handlesAValidReadingAsTheGatewayOfTheTopicAndAcknowledgesAfterwards() {
        List<Authentication> duringHandle = new ArrayList<>();
        when(readings.handle(any())).thenAnswer(invocation -> {
            duringHandle.add(SecurityContextHolder.getContext().getAuthentication());
            return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
        });

        listener.onReading(TOPIC, bytes(VALID), acknowledge);

        ArgumentCaptor<ProcessSensorReadingCommand> command = ArgumentCaptor.forClass(ProcessSensorReadingCommand.class);
        InOrder order = inOrder(readings, acknowledge);
        order.verify(readings).handle(command.capture());
        order.verify(acknowledge).run();
        assertThat(command.getValue().institutionId()).isEqualTo(7L);
        assertThat(command.getValue().reading().hardwareId()).isEqualTo("SN-1");
        assertThat(command.getValue().reading().kind()).isEqualTo(ReadingKind.SPACE);
        assertThat(command.getValue().reading().gatewayHardwareId()).isEqualTo("GW-01");
        assertThat(duringHandle).singleElement().satisfies(authentication -> {
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getPrincipal()).isEqualTo(new VerifiedGatewayPrincipal(7L, "GW-01"));
        });
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(rejected);
    }

    @Test
    void storesAMalformedMessageAsRejectedAndAcknowledgesItWithoutProcessing() {
        listener.onReading(TOPIC, bytes("{\"kind\":\"SPACE\"}"), acknowledge);

        InOrder order = inOrder(rejected, acknowledge);
        order.verify(rejected).store(eq(TOPIC), eq("{\"kind\":\"SPACE\"}"), startsWith("Malformed message: "), eq(NOW));
        order.verify(acknowledge).run();
        verifyNoInteractions(readings);
    }

    @Test
    void storesAMessageOnAnUnknownTopicAsRejectedAndAcknowledgesIt() {
        listener.onReading("quadrapp/not-a-number/GW-01/readings", bytes(VALID), acknowledge);

        verify(rejected).store(eq("quadrapp/not-a-number/GW-01/readings"), eq(VALID),
                startsWith("Malformed message: "), eq(NOW));
        verify(acknowledge).run();
        verifyNoInteractions(readings);
    }

    @Test
    void retriesAFailingReadingThreeTimesThenStoresItAsRejectedAndAcknowledges() {
        when(readings.handle(any())).thenThrow(new DataAccessResourceFailureException("database down"));

        listener.onReading(TOPIC, bytes(VALID), acknowledge);

        InOrder order = inOrder(readings, rejected, acknowledge);
        order.verify(readings, times(1 + MqttInboundListener.MAX_RETRIES)).handle(any());
        order.verify(rejected).store(eq(TOPIC), eq(VALID),
                eq("Processing failed after 4 attempts: DataAccessResourceFailureException: database down"), eq(NOW));
        order.verify(acknowledge).run();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void acknowledgesWithoutRejectingWhenARetrySucceeds() {
        when(readings.handle(any()))
                .thenThrow(new DataAccessResourceFailureException("database down"))
                .thenReturn(new SensorReadingResult(ReadingOutcome.PENDING, null, null));

        listener.onReading(TOPIC, bytes(VALID), acknowledge);

        verify(readings, times(2)).handle(any());
        verify(acknowledge).run();
        verifyNoInteractions(rejected);
    }

    @Test
    void doesNotAcknowledgeWhenTheRejectedMessageCannotBeStored() {
        doThrow(new DataAccessResourceFailureException("database down"))
                .when(rejected).store(anyString(), anyString(), anyString(), any());

        assertThatThrownBy(() -> listener.onReading(TOPIC, bytes("not json"), acknowledge))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(acknowledge, never()).run();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingKind;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorReadingTranslatorTest {

    private static final String TOPIC = "quadrapp/7/GW-01/readings";
    private static final String EVENT_ID = "8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10";

    private final SensorReadingTranslator translator = new SensorReadingTranslator();

    @Test
    void translatesASpaceReadingWithTheInstitutionAndGatewayOfTheTopic() {
        GatewayReading translated = translator.translate(TOPIC, bytes("""
                {"eventId":"%s","hardwareId":"SN-0342","kind":"SPACE","state":"OCCUPIED",
                 "readingAt":"2026-10-09T13:05:12.200Z","battery":82,"signal":-71}
                """.formatted(EVENT_ID)));

        assertThat(translated.institutionId()).isEqualTo(7L);
        assertThat(translated.reading()).isEqualTo(new SensorReading(EVENT_ID, "SN-0342", ReadingKind.SPACE,
                SensorState.OCCUPIED, null, Instant.parse("2026-10-09T13:05:12.200Z"), 82, -71, "GW-01"));
    }

    @Test
    void translatesAPassReadingWithoutOptionalFields() {
        GatewayReading translated = translator.translate(TOPIC, bytes("""
                {"eventId":"%s","hardwareId":"PS-01","kind":"PASS","direction":"OUT",
                 "readingAt":"2026-10-09T13:05:12Z"}
                """.formatted(EVENT_ID)));

        assertThat(translated.reading()).isEqualTo(new SensorReading(EVENT_ID, "PS-01", ReadingKind.PASS,
                null, PassDirection.OUT, Instant.parse("2026-10-09T13:05:12Z"), null, null, "GW-01"));
    }

    @Test
    void translatesAHeartbeatAndTreatsNullOptionalFieldsAsAbsent() {
        GatewayReading translated = translator.translate(TOPIC, bytes("""
                {"eventId":"%s","hardwareId":"GW-01","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z",
                 "battery":null,"signal":null}
                """.formatted(EVENT_ID)));

        assertThat(translated.reading()).isEqualTo(new SensorReading(EVENT_ID, "GW-01", ReadingKind.HEARTBEAT,
                null, null, Instant.parse("2026-10-09T13:05:12Z"), null, null, "GW-01"));
    }

    @Test
    void takesTheGatewayFromTheTopicAndNeverFromThePayload() {
        GatewayReading translated = translator.translate(TOPIC, bytes("""
                {"eventId":"%s","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z",
                 "gatewayHardwareId":"GW-OTHER","gatewayId":"GW-OTHER"}
                """.formatted(EVENT_ID)));

        assertThat(translated.reading().gatewayHardwareId()).isEqualTo("GW-01");
    }

    @Test
    void normalizesTheEventIdToTheCanonicalLowerCaseUuid() {
        GatewayReading translated = translator.translate(TOPIC, bytes("""
                {"eventId":"8F0D5C0E-3B1A-4D7E-9A35-2F8B1C7E4A10","hardwareId":"SN-1","kind":"HEARTBEAT",
                 "readingAt":"2026-10-09T13:05:12Z"}
                """));

        assertThat(translated.reading().eventId()).isEqualTo(EVENT_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "quadrapp/7/GW-01/status",
            "quadrapp/7/readings",
            "quadrapp/7/GW-01/extra/readings",
            "other/7/GW-01/readings",
            "quadrapp/abc/GW-01/readings",
            "quadrapp/0/GW-01/readings",
            "quadrapp/-3/GW-01/readings",
            "quadrapp/99999999999999999999/GW-01/readings",
            "quadrapp/7/ /readings",
            "quadrapp/7//readings"
    })
    void rejectsATopicOutsideTheReadingsContract(String topic) {
        byte[] valid = bytes("""
                {"eventId":"%s","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z"}
                """.formatted(EVENT_ID));

        assertThatThrownBy(() -> translator.translate(topic, valid))
                .isInstanceOf(MalformedReadingException.class);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            not json                  | {
            not an object             | ["SPACE"]
            empty payload             | ''
            missing eventId           | {"hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z"}
            eventId not a UUID        | {"eventId":"evt-1","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z"}
            eventId not a string      | {"eventId":12,"hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z"}
            missing hardwareId        | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z"}
            blank hardwareId          | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":" ","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z"}
            missing kind              | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","readingAt":"2026-10-09T13:05:12Z"}
            unknown kind              | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"DOOR","readingAt":"2026-10-09T13:05:12Z"}
            SPACE without state       | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"SPACE","readingAt":"2026-10-09T13:05:12Z"}
            unknown state             | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"SPACE","state":"BUSY","readingAt":"2026-10-09T13:05:12Z"}
            PASS with state           | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"PASS","state":"FREE","direction":"IN","readingAt":"2026-10-09T13:05:12Z"}
            PASS without direction    | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"PASS","readingAt":"2026-10-09T13:05:12Z"}
            HEARTBEAT with state      | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT","state":"FREE","readingAt":"2026-10-09T13:05:12Z"}
            missing readingAt         | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT"}
            readingAt not an instant  | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09 13:05"}
            battery above 100         | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z","battery":101}
            battery below 0           | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z","battery":-1}
            battery not an integer    | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z","battery":"82"}
            signal not an integer     | {"eventId":"8f0d5c0e-3b1a-4d7e-9a35-2f8b1c7e4a10","hardwareId":"SN-1","kind":"HEARTBEAT","readingAt":"2026-10-09T13:05:12Z","signal":-71.5}
            """)
    void rejectsAPayloadOutsideTheReadingContract(String description, String payload) {
        assertThatThrownBy(() -> translator.translate(TOPIC, bytes(payload)))
                .as(description)
                .isInstanceOf(MalformedReadingException.class)
                .hasMessageNotContaining("not implemented");
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.PassDirection;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.ReadingKind;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorState;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Anti-corruption layer between the gateway message format and the Parking Sensing domain.
 * <p>
 * It is the only component that knows the gateway contract: the topic
 * {@code quadrapp/{tenantId}/{gatewayId}/readings} and the JSON payload
 * ({@code eventId}, {@code hardwareId}, {@code kind}, {@code state}, {@code direction},
 * {@code readingAt}, {@code battery}, {@code signal}). Another sensing technology only needs
 * another translator (CON-16). The gateway identity is taken from the topic, which the broker
 * restricts per gateway, and never from the payload. Unknown payload fields are ignored.
 */
public class SensorReadingTranslator {

    static final String TOPIC_FORMAT = "quadrapp/{tenantId}/{gatewayId}/readings";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern POSITIVE_NUMBER = Pattern.compile("[1-9][0-9]{0,18}");
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public GatewayReading translate(String topic, byte[] payload) {
        String[] levels = topic == null ? new String[0] : topic.split("/", -1);
        if (levels.length != 4 || !"quadrapp".equals(levels[0]) || !"readings".equals(levels[3])) {
            throw new MalformedReadingException("Topic must be " + TOPIC_FORMAT + ".");
        }
        Long institutionId = institutionId(levels[1]);
        String gatewayHardwareId = levels[2];
        if (gatewayHardwareId.isBlank()) {
            throw new MalformedReadingException("Topic gatewayId is required.");
        }

        JsonNode json = parse(payload);
        try {
            SensorReading reading = new SensorReading(
                    eventId(json),
                    requiredText(json, "hardwareId"),
                    enumValue(json, "kind", ReadingKind.class, true),
                    enumValue(json, "state", SensorState.class, false),
                    enumValue(json, "direction", PassDirection.class, false),
                    readingAt(json),
                    optionalInteger(json, "battery"),
                    optionalInteger(json, "signal"),
                    gatewayHardwareId
            );
            return new GatewayReading(institutionId, reading);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            // The value object enforces the remaining rules (kind and state/direction, battery
            // range, hardware identifier length); a violation is a format error of the message.
            throw new MalformedReadingException(invalid.getMessage());
        }
    }

    private static Long institutionId(String level) {
        if (!POSITIVE_NUMBER.matcher(level).matches()) {
            throw new MalformedReadingException("Topic tenantId must be a positive institution id.");
        }
        try {
            return Long.parseLong(level);
        } catch (NumberFormatException overflow) {
            throw new MalformedReadingException("Topic tenantId must be a positive institution id.");
        }
    }

    private static JsonNode parse(byte[] payload) {
        JsonNode json;
        try {
            json = payload == null || payload.length == 0 ? null : JSON.readTree(payload);
        } catch (JacksonException invalid) {
            throw new MalformedReadingException("Payload is not valid JSON.");
        }
        if (json == null || !json.isObject()) {
            throw new MalformedReadingException("Payload must be a JSON object.");
        }
        return json;
    }

    private static String eventId(JsonNode json) {
        String value = requiredText(json, "eventId").trim();
        if (!UUID_TEXT.matcher(value).matches()) {
            throw new MalformedReadingException("eventId must be a UUID.");
        }
        return UUID.fromString(value).toString();
    }

    private static Instant readingAt(JsonNode json) {
        String value = requiredText(json, "readingAt");
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException invalid) {
            throw new MalformedReadingException("readingAt must be an ISO-8601 instant.");
        }
    }

    private static <E extends Enum<E>> E enumValue(JsonNode json, String field, Class<E> type, boolean required) {
        JsonNode node = json.get(field);
        if (isAbsent(node)) {
            if (required) {
                throw new MalformedReadingException(field + " is required.");
            }
            return null;
        }
        String allowed = Arrays.toString(type.getEnumConstants());
        if (!node.isString()) {
            throw new MalformedReadingException(field + " must be one of " + allowed + ".");
        }
        try {
            return Enum.valueOf(type, node.stringValue());
        } catch (IllegalArgumentException unknown) {
            throw new MalformedReadingException(field + " must be one of " + allowed + ".");
        }
    }

    private static String requiredText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (isAbsent(node)) {
            throw new MalformedReadingException(field + " is required.");
        }
        if (!node.isString()) {
            throw new MalformedReadingException(field + " must be a string.");
        }
        return node.stringValue();
    }

    private static Integer optionalInteger(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (isAbsent(node)) {
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new MalformedReadingException(field + " must be an integer.");
        }
        return node.intValue();
    }

    private static boolean isAbsent(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }
}

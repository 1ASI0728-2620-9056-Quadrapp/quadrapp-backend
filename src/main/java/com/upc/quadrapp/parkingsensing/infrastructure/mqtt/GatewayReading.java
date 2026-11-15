package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;

/**
 * A reading translated from a gateway message, with the institution taken from its topic.
 * The reading's {@code gatewayHardwareId} also comes from the topic, never from the payload.
 */
public record GatewayReading(Long institutionId, SensorReading reading) {
}

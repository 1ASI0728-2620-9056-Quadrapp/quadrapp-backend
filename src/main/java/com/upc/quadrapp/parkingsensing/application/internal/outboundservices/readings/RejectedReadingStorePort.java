package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings;

import java.time.Instant;

/**
 * Keeps the gateway messages that could not be processed (malformed, or failing after every
 * retry), so they can be inspected after they were acknowledged to the broker.
 */
public interface RejectedReadingStorePort {
    void store(String topic, String payload, String reason, Instant receivedAt);
}

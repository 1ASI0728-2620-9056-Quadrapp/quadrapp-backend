package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

/**
 * The gateway message does not follow the reading contract (topic, JSON shape, types or
 * allowed values). Retrying it cannot succeed, so it is stored as rejected and acknowledged.
 */
public class MalformedReadingException extends RuntimeException {
    public MalformedReadingException(String message) {
        super(message);
    }
}

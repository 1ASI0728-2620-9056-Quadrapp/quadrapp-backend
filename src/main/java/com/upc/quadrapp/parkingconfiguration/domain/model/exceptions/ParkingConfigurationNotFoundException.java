package com.upc.quadrapp.parkingconfiguration.domain.model.exceptions;

/**
 * The parking lot, zone or level named in the request does not exist, or belongs to an
 * institution the caller cannot see (HTTP 404 in both cases, so existence is not revealed).
 */
public class ParkingConfigurationNotFoundException extends RuntimeException {

    public ParkingConfigurationNotFoundException(String message) {
        super(message);
    }
}

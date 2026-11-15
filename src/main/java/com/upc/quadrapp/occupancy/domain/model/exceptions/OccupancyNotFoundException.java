package com.upc.quadrapp.occupancy.domain.model.exceptions;

public class OccupancyNotFoundException extends RuntimeException {

    public OccupancyNotFoundException(String message) {
        super(message);
    }
}

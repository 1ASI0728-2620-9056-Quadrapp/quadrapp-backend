package com.upc.quadrapp.prediction.domain.model.exceptions;

public class PredictionNotFoundException extends RuntimeException {

    public PredictionNotFoundException(String message) {
        super(message);
    }
}

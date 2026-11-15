package com.upc.quadrapp.prediction.domain.model.valueobjects;

import java.util.List;

public enum ForecastHorizon {
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    FORTY_FIVE_MINUTES(45),
    SIXTY_MINUTES(60);

    private static final List<ForecastHorizon> ALL = List.of(values());

    private final int minutes;

    ForecastHorizon(int minutes) {
        this.minutes = minutes;
    }

    public int minutes() {
        return minutes;
    }

    public static List<ForecastHorizon> all() {
        return ALL;
    }
}

package com.upc.quadrapp.prediction.infrastructure.scheduling;

import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ForecastEvaluationScheduler {

    private final ForecastCommandService forecasts;

    public ForecastEvaluationScheduler(ForecastCommandService forecasts) {
        this.forecasts = forecasts;
    }

    @Scheduled(fixedDelay = 60_000)
    public void evaluateDueForecasts() {
        forecasts.evaluateDueForecasts();
    }
}

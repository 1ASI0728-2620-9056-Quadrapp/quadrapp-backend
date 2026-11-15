package com.upc.quadrapp.prediction.infrastructure.scheduling;

import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ForecastEvaluationScheduler {

    private final ForecastCommandService forecasts;

    public ForecastEvaluationScheduler(ForecastCommandService forecasts) {
        this.forecasts = forecasts;
    }

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "forecast-evaluation", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void evaluateDueForecasts() {
        forecasts.evaluateDueForecasts();
    }
}

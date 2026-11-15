package com.upc.quadrapp.prediction.infrastructure.scheduling;

import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Forecast runs every 5 minutes for every active parking lot, and the daily purge of forecasts
 * older than 7 days. Each job runs in one replica at a time (ShedLock); the unique scheduled run
 * per slot also stops a second replica from saving the same run.
 */
@Component
public class ForecastScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastScheduler.class);

    private final ForecastCommandService forecasts;

    public ForecastScheduler(ForecastCommandService forecasts) {
        this.forecasts = forecasts;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "forecast-runs", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void generateForecasts() {
        int runs = forecasts.generateScheduledRuns();
        LOGGER.debug("Generated {} scheduled forecast runs.", runs);
    }

    @Scheduled(cron = "0 23 4 * * *")
    @SchedulerLock(name = "forecast-purge", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void purgeExpiredForecasts() {
        int deleted = forecasts.purgeExpiredForecasts();
        LOGGER.debug("Purged {} forecasts older than 7 days.", deleted);
    }
}

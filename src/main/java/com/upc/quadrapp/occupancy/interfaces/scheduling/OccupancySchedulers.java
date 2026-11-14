package com.upc.quadrapp.occupancy.interfaces.scheduling;

import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs of Occupancy. Each runs in one replica at a time (ShedLock).
 */
@Component
public class OccupancySchedulers {

    private static final Logger LOGGER = LoggerFactory.getLogger(OccupancySchedulers.class);

    private final OccupancyCommandService commands;

    public OccupancySchedulers(OccupancyCommandService commands) {
        this.commands = commands;
    }

    /** One snapshot per zone and minute, the history Prediction reads (kept 8 weeks). */
    @Scheduled(cron = "5 * * * * *")
    @SchedulerLock(name = "occupancy-minute-snapshots", lockAtMostFor = "PT50S", lockAtLeastFor = "PT10S")
    public void recordSnapshots() {
        commands.recordMinuteSnapshots();
    }

    /** Deletes snapshots older than 8 weeks and passages and sensor events older than 7 days. */
    @Scheduled(cron = "0 17 * * * *")
    @SchedulerLock(name = "occupancy-history-purge", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void purgeExpiredHistory() {
        int deleted = commands.purgeExpiredHistory();
        LOGGER.debug("Purged {} expired occupancy history rows.", deleted);
    }
}

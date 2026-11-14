package com.upc.quadrapp.shared.infrastructure.events;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Keeps the event publication registry healthy while the application runs:
 * <ul>
 *     <li>Publications whose listener failed are delivered again every minute, not only on
 *     restart, up to {@code quadrapp.events.retry.max-attempts} deliveries; a publication that
 *     reaches the limit stays FAILED and is logged for an operator.</li>
 *     <li>Completed publications are deleted after {@code quadrapp.events.completed-retention}.</li>
 * </ul>
 * Listeners drop duplicates by {@code eventId}, so a publication delivered again (or delivered by
 * two replicas at once) completes without applying its change twice.
 */
@Component
public class EventPublicationMaintenance {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublicationMaintenance.class);

    private final FailedEventPublications failedPublications;
    private final CompletedEventPublications completedPublications;
    private final int maxAttempts;
    private final Duration minAge;
    private final Duration completedRetention;

    public EventPublicationMaintenance(
            FailedEventPublications failedPublications,
            CompletedEventPublications completedPublications,
            @Value("${quadrapp.events.retry.max-attempts:5}") int maxAttempts,
            @Value("${quadrapp.events.retry.min-age:PT1M}") Duration minAge,
            @Value("${quadrapp.events.completed-retention:P7D}") Duration completedRetention
    ) {
        this.failedPublications = failedPublications;
        this.completedPublications = completedPublications;
        this.maxAttempts = maxAttempts;
        this.minAge = minAge;
        this.completedRetention = completedRetention;
    }

    @Scheduled(fixedDelayString = "${quadrapp.events.retry.interval:PT1M}", initialDelayString = "PT1M")
    @SchedulerLock(name = "event-publication-retry", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void retryFailedPublications() {
        failedPublications.resubmit(ResubmissionOptions.defaults()
                .withMinAge(minAge)
                .withBatchSize(100)
                .withFilter(this::belowMaxAttempts));
    }

    @Scheduled(cron = "0 41 * * * *")
    @SchedulerLock(name = "event-publication-cleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void deleteCompletedPublications() {
        completedPublications.deletePublicationsOlderThan(completedRetention);
    }

    private boolean belowMaxAttempts(EventPublication publication) {
        if (publication.getCompletionAttempts() < maxAttempts) {
            return true;
        }
        LOGGER.warn("Event publication {} ({}) failed {} times and is no longer retried.",
                publication.getIdentifier(), publication.getEvent().getClass().getSimpleName(),
                publication.getCompletionAttempts());
        return false;
    }
}

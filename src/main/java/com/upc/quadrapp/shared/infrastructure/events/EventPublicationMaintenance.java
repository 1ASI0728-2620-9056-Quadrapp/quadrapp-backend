package com.upc.quadrapp.shared.infrastructure.events;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
 *     <li>Publications left waiting, in progress or resubmitted for longer than the staleness
 *     limits ({@code spring.modulith.events.staleness.*}), for example by a replica that stopped,
 *     are marked FAILED first.</li>
 *     <li>Publications whose listener failed are delivered again every minute, up to
 *     {@code quadrapp.events.retry.max-attempts} deliveries; a publication that reaches the limit
 *     stays FAILED and is logged for an operator. Nothing is republished on restart, so this job
 *     is the only redelivery and its limit always applies.</li>
 *     <li>Completed publications are deleted after {@code quadrapp.events.completed-retention}.</li>
 * </ul>
 * Listeners drop duplicates by {@code eventId}, so a publication delivered again (or delivered by
 * two replicas at once) completes without applying its change twice.
 */
@Component
public class EventPublicationMaintenance {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublicationMaintenance.class);

    private final JdbcTemplate jdbc;
    private final Duration publishedStaleAfter;
    private final Duration processingStaleAfter;
    private final Duration resubmittedStaleAfter;
    private final FailedEventPublications failedPublications;
    private final CompletedEventPublications completedPublications;
    private final int maxAttempts;
    private final Duration minAge;
    private final Duration completedRetention;

    public EventPublicationMaintenance(
            JdbcTemplate jdbc,
            @Value("${spring.modulith.events.staleness.published:PT10M}") Duration publishedStaleAfter,
            @Value("${spring.modulith.events.staleness.processing:PT10M}") Duration processingStaleAfter,
            @Value("${spring.modulith.events.staleness.resubmitted:PT10M}") Duration resubmittedStaleAfter,
            FailedEventPublications failedPublications,
            CompletedEventPublications completedPublications,
            @Value("${quadrapp.events.retry.max-attempts:5}") int maxAttempts,
            @Value("${quadrapp.events.retry.min-age:PT1M}") Duration minAge,
            @Value("${quadrapp.events.completed-retention:P7D}") Duration completedRetention
    ) {
        this.jdbc = jdbc;
        this.publishedStaleAfter = publishedStaleAfter;
        this.processingStaleAfter = processingStaleAfter;
        this.resubmittedStaleAfter = resubmittedStaleAfter;
        this.failedPublications = failedPublications;
        this.completedPublications = completedPublications;
        this.maxAttempts = maxAttempts;
        this.minAge = minAge;
        this.completedRetention = completedRetention;
    }

    @Scheduled(fixedDelayString = "${quadrapp.events.retry.interval:PT1M}", initialDelayString = "PT1M")
    @SchedulerLock(name = "event-publication-retry", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void retryFailedPublications() {
        markStalePublicationsFailed();
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

    /**
     * Marks FAILED the incomplete publications stuck longer than their staleness limit, so the
     * resubmission below picks them up. The table is the JDBC registry of Spring Modulith in the
     * shared schema.
     */
    private void markStalePublicationsFailed() {
        int marked = 0;
        for (var limit : java.util.Map.of(
                "PUBLISHED", publishedStaleAfter,
                "PROCESSING", processingStaleAfter,
                "RESUBMITTED", resubmittedStaleAfter).entrySet()) {
            marked += jdbc.update("""
                    UPDATE shared.event_publication
                    SET status = 'FAILED'
                    WHERE completion_date IS NULL
                      AND status = ?
                      AND COALESCE(last_resubmission_date, publication_date) < now() - make_interval(secs => ?)
                    """, limit.getKey(), limit.getValue().toSeconds());
        }
        if (marked > 0) {
            LOGGER.warn("Marked {} stale event publications as FAILED so they are delivered again.", marked);
        }
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

package com.upc.quadrapp.occupancy.infrastructure.events;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.events.OccupancyEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes Occupancy's domain events as Spring application events. Inside a transaction, the
 * event publication registry stores them for every {@code @ApplicationModuleListener}, which
 * receives them after the commit.
 */
@Component
public class OccupancyEventPublisherAdapter implements OccupancyEventPublisherPort {

    private final ApplicationEventPublisher events;

    public OccupancyEventPublisherAdapter(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(List<Object> domainEvents) {
        domainEvents.forEach(events::publishEvent);
    }
}

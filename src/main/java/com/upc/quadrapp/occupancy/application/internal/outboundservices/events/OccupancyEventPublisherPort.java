package com.upc.quadrapp.occupancy.application.internal.outboundservices.events;

import java.util.List;

public interface OccupancyEventPublisherPort {

    /** Publishes the domain events inside the current transaction (event publication registry). */
    void publish(List<Object> events);
}

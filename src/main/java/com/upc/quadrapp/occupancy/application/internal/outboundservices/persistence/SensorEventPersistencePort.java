package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.entities.SensorEvent;

import java.time.Instant;

public interface SensorEventPersistencePort {

    boolean insertIfAbsent(SensorEvent event);

    int deleteReceivedBefore(Instant cutoff);
}

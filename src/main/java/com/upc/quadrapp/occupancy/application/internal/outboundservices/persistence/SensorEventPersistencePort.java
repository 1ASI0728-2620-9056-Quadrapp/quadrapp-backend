package com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence;

import com.upc.quadrapp.occupancy.domain.model.entities.SensorEvent;

public interface SensorEventPersistencePort {

    boolean insertIfAbsent(SensorEvent event);
}

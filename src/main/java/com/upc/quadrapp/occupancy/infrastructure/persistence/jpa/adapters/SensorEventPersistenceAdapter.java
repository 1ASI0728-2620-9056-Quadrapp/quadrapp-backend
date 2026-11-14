package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SensorEventPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.entities.SensorEvent;
import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories.SensorEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SensorEventPersistenceAdapter implements SensorEventPersistencePort {

    private final SensorEventJpaRepository repository;

    public SensorEventPersistenceAdapter(SensorEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean insertIfAbsent(SensorEvent event) {
        return repository.insertIfAbsent(
                event.eventId(),
                event.deviceId(),
                event.spaceId(),
                event.accessId(),
                event.eventType().name(),
                event.occurredAt(),
                event.receivedAt()
        ) == 1;
    }

    @Override
    public int deleteReceivedBefore(java.time.Instant cutoff) {
        return repository.deleteReceivedBefore(cutoff);
    }
}

package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Level;

import java.util.List;
import java.util.Optional;

public interface LevelPersistencePort {

    Level save(Level value);

    Optional<Level> findById(Long id);

    List<Level> findByZoneId(Long zoneId);

    boolean existsByZoneIdAndName(
            Long zoneId,
            String name
    );
}

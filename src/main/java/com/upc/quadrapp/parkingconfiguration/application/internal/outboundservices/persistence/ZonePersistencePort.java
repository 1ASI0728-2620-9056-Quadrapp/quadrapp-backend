package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Zone;

import java.util.List;
import java.util.Optional;

public interface ZonePersistencePort {

    Zone save(Zone value);

    Optional<Zone> findById(Long id);

    List<Zone> findByParkingLotId(Long parkingLotId);

    boolean existsByParkingLotIdAndName(
            Long parkingLotId,
            String name
    );
}

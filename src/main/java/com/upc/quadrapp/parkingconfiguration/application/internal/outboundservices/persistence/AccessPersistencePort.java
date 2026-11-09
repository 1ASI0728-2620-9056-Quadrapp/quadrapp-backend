package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Access;

import java.util.List;
import java.util.Optional;

public interface AccessPersistencePort {

    Access save(Access value);

    Optional<Access> findById(Long id);

    List<Access> findByParkingLotId(Long parkingLotId);

    boolean existsByParkingLotIdAndName(
            Long parkingLotId,
            String name
    );
}

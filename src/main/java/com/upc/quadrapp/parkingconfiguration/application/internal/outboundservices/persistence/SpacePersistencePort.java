package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Space;

import java.util.List;
import java.util.Optional;

public interface SpacePersistencePort {

    Space save(Space value);

    Optional<Space> findById(Long id);

    List<Space> findByLevelId(Long levelId);

    boolean existsByLevelIdAndLabel(
            Long levelId,
            String label
    );
}

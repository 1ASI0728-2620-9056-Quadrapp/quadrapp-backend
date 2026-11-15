package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;

import java.util.List;
import java.util.Optional;

public interface ParkingLotPersistencePort {

    ParkingLot save(ParkingLot value);

    Optional<ParkingLot> findById(Long id);

    List<ParkingLot> findByInstitutionId(Long institutionId);

    /** IDs of the parking lots in service, of every institution. */
    List<Long> findActiveIds();

    boolean existsByInstitutionIdAndName(
            Long institutionId,
            String name
    );
}

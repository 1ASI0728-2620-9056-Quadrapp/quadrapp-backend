package com.upc.quadrapp.parkingconfiguration.domain.services;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface ParkingConfigurationQueryService {

    Optional<ParkingLot> handle(GetParkingLotByIdQuery query);

    List<ParkingLot> handle(GetParkingLotsByInstitutionQuery query);

    List<Zone> handle(GetZonesByParkingLotQuery query);

    List<Level> handle(GetLevelsByZoneQuery query);

    List<Space> handle(GetSpacesByLevelQuery query);

    List<Access> handle(GetAccessesByParkingLotQuery query);
}

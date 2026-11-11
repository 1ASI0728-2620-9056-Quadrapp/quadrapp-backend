package com.upc.quadrapp.parkingconfiguration.interfaces.acl;

import com.upc.quadrapp.parkingconfiguration.domain.model.queries.*;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkingConfigurationContextFacade {

    private final ParkingConfigurationQueryService queries;

    public Optional<ParkingLotView> getParkingLot(
            Long parkingLotId
    ) {
        return queries.handle(
                new GetParkingLotByIdQuery(parkingLotId)
        ).map(value ->
                new ParkingLotView(
                        value.getId(),
                        value.getInstitutionId(),
                        value.getName()
                )
        );
    }

    public List<ParkingLotView> getParkingLotsByInstitution(
            Long institutionId
    ) {
        return queries.handle(
                        new GetParkingLotsByInstitutionQuery(institutionId)
                ).stream()
                .map(value ->
                        new ParkingLotView(
                                value.getId(),
                                value.getInstitutionId(),
                                value.getName()
                        )
                )
                .toList();
    }

    public List<SpaceView> getSpacesByLevel(
            Long levelId
    ) {
        return queries.handle(
                        new GetSpacesByLevelQuery(levelId)
                ).stream()
                .map(value ->
                        new SpaceView(
                                value.getId(),
                                value.getLevelId(),
                                value.getLabel()
                        )
                )
                .toList();
    }

    public record ParkingLotView(
            Long id,
            Long institutionId,
            String name
    ) {}

    public record SpaceView(
            Long id,
            Long levelId,
            String label
    ) {}
}

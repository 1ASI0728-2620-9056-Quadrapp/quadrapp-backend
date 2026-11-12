package com.upc.quadrapp.parkingconfiguration.interfaces.acl;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.ParkingLotPersistencePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkingConfigurationContextFacade {

    private final ParkingLotPersistencePort parkingLots;
    private final ParkingConfigurationAuthorizationPort authorization;

    public Optional<ParkingLotView> findParkingLot(
            Long institutionId,
            Long parkingLotId
    ) {
        Objects.requireNonNull(
                institutionId,
                "Institution ID is required."
        );

        Objects.requireNonNull(
                parkingLotId,
                "Parking lot ID is required."
        );

        authorization.requireInstitutionRead(institutionId);

        return parkingLots.findById(parkingLotId)
                .filter(lot ->
                        institutionId.equals(lot.getInstitutionId())
                )
                .map(lot ->
                        new ParkingLotView(
                                lot.getId(),
                                lot.getInstitutionId()
                        )
                );
    }

    public boolean belongsToInstitution(
            Long institutionId,
            Long parkingLotId
    ) {
        return findParkingLot(
                institutionId,
                parkingLotId
        ).isPresent();
    }

    public record ParkingLotView(
            Long id,
            Long institutionId
    ) {}
}

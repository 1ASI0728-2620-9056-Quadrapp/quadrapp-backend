package com.upc.quadrapp.parkingconfiguration.interfaces.acl;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.CalendarEventPersistencePort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.ParkingLotPersistencePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ParkingConfigurationContextFacade {

    private final ParkingLotPersistencePort parkingLots;
    private final CalendarEventPersistencePort calendarEvents;
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

    public Optional<PredictionConfigurationView> findPredictionConfiguration(
            Long institutionId,
            Long parkingLotId,
            Instant from,
            Instant to
    ) {
        Objects.requireNonNull(institutionId, "Institution ID is required.");
        Objects.requireNonNull(parkingLotId, "Parking lot ID is required.");
        Objects.requireNonNull(from, "Start of prediction window is required.");
        Objects.requireNonNull(to, "End of prediction window is required.");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Prediction window must have positive duration.");
        }

        authorization.requireInstitutionRead(institutionId);

        return parkingLots.findById(parkingLotId)
                .filter(lot -> institutionId.equals(lot.getInstitutionId()))
                .map(lot -> {
                    List<CalendarEventView> events = calendarEvents
                            .findOverlappingByInstitutionId(institutionId, from, to)
                            .stream()
                            .filter(event -> event.getCancelledAt() == null)
                            .filter(event -> event.getParkingLotId() == null
                                    || parkingLotId.equals(event.getParkingLotId()))
                            .map(event -> new CalendarEventView(
                                    event.getType().name(),
                                    event.getImpact().name(),
                                    event.getWindow().startAt(),
                                    event.getWindow().endAt()
                            ))
                            .toList();

                    return new PredictionConfigurationView(
                            lot.getSaturationThresholdPct(),
                            lot.getAccuracyMarginPct(),
                            lot.getFlowWindowMinutes(),
                            lot.getOpensAt(),
                            lot.getClosesAt(),
                            events
                    );
                });
    }

    public record ParkingLotView(
            Long id,
            Long institutionId
    ) {}

    public record PredictionConfigurationView(
            int saturationThresholdPct,
            int accuracyMarginPct,
            int flowWindowMinutes,
            LocalTime opensAt,
            LocalTime closesAt,
            List<CalendarEventView> calendarEvents
    ) {}

    public record CalendarEventView(
            String type,
            String impact,
            Instant startsAt,
            Instant endsAt
    ) {}
}

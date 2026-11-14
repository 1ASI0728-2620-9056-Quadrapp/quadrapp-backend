package com.upc.quadrapp.parkingconfiguration.interfaces.acl;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.AccessPersistencePort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.CalendarEventPersistencePort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.LevelPersistencePort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.ParkingLotPersistencePort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.SpacePersistencePort;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.ZonePersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ZonePersistencePort zones;
    private final LevelPersistencePort levels;
    private final SpacePersistencePort spaces;
    private final AccessPersistencePort accesses;

    /**
     * The parking lot if the authenticated user can read it. A lot of another institution looks
     * exactly like a missing one, so callers can answer 404 without revealing that it exists.
     */
    public Optional<ParkingLotView> findReadableParkingLot(Long parkingLotId) {
        Objects.requireNonNull(parkingLotId, "Parking lot ID is required.");
        return parkingLots.findById(parkingLotId)
                .filter(lot -> authorization.canRead(lot.getInstitutionId()))
                .map(ParkingConfigurationContextFacade::view);
    }

    /** The readable parking lot that holds the zone; empty if missing or of another institution. */
    public Optional<ParkingLotView> findReadableParkingLotOfZone(Long zoneId) {
        Objects.requireNonNull(zoneId, "Zone ID is required.");
        return zones.findById(zoneId)
                .flatMap(zone -> findReadableParkingLot(zone.getParkingLotId()));
    }

    /** The readable parking lot that holds the level; empty if missing or of another institution. */
    public Optional<ParkingLotView> findReadableParkingLotOfLevel(Long levelId) {
        Objects.requireNonNull(levelId, "Level ID is required.");
        return levels.findById(levelId)
                .flatMap(level -> zones.findById(level.getZoneId()))
                .flatMap(zone -> findReadableParkingLot(zone.getParkingLotId()));
    }

    /**
     * Current layout of a parking lot for Occupancy: zones, levels and spaces (with their active
     * flag), its accesses and the low-availability threshold. It is an internal read for the
     * modules that keep their own copy of the layout, so it does not check the caller.
     */
    @Transactional(readOnly = true)
    public Optional<ParkingLayoutView> layoutOf(Long parkingLotId) {
        Objects.requireNonNull(parkingLotId, "Parking lot ID is required.");
        return parkingLots.findById(parkingLotId).map(lot -> new ParkingLayoutView(
                lot.getId(),
                lot.getInstitutionId(),
                lot.isActive(),
                lot.getLowAvailabilityPct(),
                zones.findByParkingLotId(lot.getId()).stream()
                        .map(zone -> new ZoneLayoutView(
                                zone.getId(),
                                levels.findByZoneId(zone.getId()).stream()
                                        .map(level -> new LevelLayoutView(
                                                level.getId(),
                                                spaces.findByLevelId(level.getId()).stream()
                                                        .map(space -> new SpaceLayoutView(
                                                                space.getId(),
                                                                space.isActive()
                                                        ))
                                                        .toList()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                accesses.findByParkingLotId(lot.getId()).stream()
                        .map(access -> new AccessLayoutView(
                                access.getId(),
                                access.isFlowCountingEnabled()
                        ))
                        .toList()
        ));
    }

    private static ParkingLotView view(ParkingLot lot) {
        return new ParkingLotView(lot.getId(), lot.getInstitutionId());
    }

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

    public record ParkingLayoutView(
            Long parkingLotId,
            Long institutionId,
            boolean active,
            int lowAvailabilityPct,
            List<ZoneLayoutView> zones,
            List<AccessLayoutView> accesses
    ) {}

    public record ZoneLayoutView(Long zoneId, List<LevelLayoutView> levels) {}

    public record LevelLayoutView(Long levelId, List<SpaceLayoutView> spaces) {}

    public record SpaceLayoutView(Long spaceId, boolean active) {}

    public record AccessLayoutView(Long accessId, boolean flowCountingEnabled) {}

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

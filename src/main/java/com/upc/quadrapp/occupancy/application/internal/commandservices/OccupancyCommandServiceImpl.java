package com.upc.quadrapp.occupancy.application.internal.commandservices;

import com.upc.quadrapp.occupancy.application.internal.commandservices.OccupancyStateManager.Recalculation;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.events.OccupancyEventPublisherPort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.LotOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancySnapshotPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SensorEventPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.SpaceOccupancyPersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.VehiclePassagePersistencePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort.SpaceChange;
import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort.ZoneOccupancyChange;
import com.upc.quadrapp.occupancy.domain.model.aggregates.LotOccupancy;
import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.aggregates.SpaceOccupancy;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateParkingLotSnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.entities.SensorEvent;
import com.upc.quadrapp.occupancy.domain.model.exceptions.OccupancyNotFoundException;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.ZoneCount;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class OccupancyCommandServiceImpl implements OccupancyCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OccupancyCommandServiceImpl.class);

    /** Per-minute snapshots feed the fallback heuristic of Prediction, which looks 8 weeks back. */
    static final Duration SNAPSHOT_RETENTION = Duration.ofDays(56);
    /** Vehicle passages and the deduplication log of sensor events. */
    static final Duration PASSAGE_RETENTION = Duration.ofDays(7);

    private final SensorEventPersistencePort sensorEvents;
    private final SpaceOccupancyPersistencePort spaces;
    private final LotOccupancyPersistencePort lots;
    private final VehiclePassagePersistencePort passages;
    private final OccupancySnapshotPersistencePort snapshots;
    private final ParkingLayoutPort layouts;
    private final OccupancyStateManager state;
    private final OccupancyEventPublisherPort events;
    private final OccupancyRealtimePort realtime;
    private final Clock clock;

    public OccupancyCommandServiceImpl(
            SensorEventPersistencePort sensorEvents,
            SpaceOccupancyPersistencePort spaces,
            LotOccupancyPersistencePort lots,
            VehiclePassagePersistencePort passages,
            OccupancySnapshotPersistencePort snapshots,
            ParkingLayoutPort layouts,
            OccupancyStateManager state,
            OccupancyEventPublisherPort events,
            OccupancyRealtimePort realtime,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.sensorEvents = sensorEvents;
        this.spaces = spaces;
        this.lots = lots;
        this.passages = passages;
        this.snapshots = snapshots;
        this.layouts = layouts;
        this.state = state;
        this.events = events;
        this.realtime = realtime;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean handle(RecordSensorEventCommand command) {
        OccupancyEventType eventType = command.eventType();
        boolean spaceEvent = eventType == OccupancyEventType.OCCUPIED
                || eventType == OccupancyEventType.FREED;

        SensorEvent event = new SensorEvent(
                command.eventId(),
                command.deviceId(),
                command.spaceId(),
                command.accessId(),
                eventType,
                command.occurredAt(),
                clock.instant()
        );

        // A duplicate delivered at the same time by another replica waits here until the first
        // one commits, and then is dropped without failing.
        if (!sensorEvents.insertIfAbsent(event)) {
            return false;
        }

        Optional<LotOccupancy> lot = state.lockFresh(command.parkingLotId())
                .filter(found -> found.getTenantId().equals(command.institutionId()));
        if (lot.isEmpty()) {
            drop(command, "its parking lot does not exist for the institution");
            return true;
        }

        if (spaceEvent) {
            applyDetection(command, lot.get(), event.receivedAt());
        } else {
            recordPassage(command, lot.get(), event.receivedAt());
        }
        return true;
    }

    private void applyDetection(RecordSensorEventCommand command, LotOccupancy lot, Instant now) {
        Optional<SpaceOccupancy> found = spaceOf(lot, command.spaceId());
        if (found.isEmpty()) {
            Optional<LotOccupancy> refreshed = state.refreshForMissingTarget(lot);
            if (refreshed.isPresent()) {
                lot = refreshed.get();
                found = spaceOf(lot, command.spaceId());
            }
        }
        if (found.isEmpty()) {
            drop(command, "its space is not in the layout of the parking lot");
            return;
        }

        SpaceOccupancy space = found.get();
        SpaceStatus presence = command.eventType() == OccupancyEventType.OCCUPIED
                ? SpaceStatus.OCCUPIED
                : SpaceStatus.FREE;
        if (!space.applyDetection(command.eventId(), presence, command.occurredAt())) {
            return;
        }
        List<Object> spaceEvents = space.pullDomainEvents();
        spaces.saveAll(List.of(space));
        if (spaceEvents.isEmpty()) {
            return;
        }
        events.publish(spaceEvents);

        Recalculation recalculation = state.recalculate(lot, now);
        ZoneOccupancyChange change = change(space, recalculation);
        afterCommit(() -> realtime.publish(change));
    }

    private void recordPassage(RecordSensorEventCommand command, LotOccupancy lot, Instant now) {
        Long parkingLotId = lot.getParkingLotId();
        boolean known = lots.hasAccessPoint(parkingLotId, command.accessId())
                || state.refreshForMissingTarget(lot).isPresent()
                && lots.hasAccessPoint(parkingLotId, command.accessId());
        if (!known) {
            drop(command, "its access is not in the layout of the parking lot");
            return;
        }
        passages.appendIfAbsent(
                command.eventId(),
                lot.getTenantId(),
                parkingLotId,
                command.accessId(),
                command.eventType() == OccupancyEventType.ENTRY,
                command.occurredAt(),
                now
        );
    }

    private Optional<SpaceOccupancy> spaceOf(LotOccupancy lot, Long spaceId) {
        return spaces.findByIdForUpdate(spaceId)
                .filter(space -> space.getParkingLotId().equals(lot.getParkingLotId()));
    }

    private static ZoneOccupancyChange change(SpaceOccupancy space, Recalculation recalculation) {
        OccupancyCounts zone = recalculation.zones().stream()
                .filter(count -> count.zoneId().equals(space.getZoneId()))
                .map(ZoneCount::counts)
                .findFirst()
                .orElse(new OccupancyCounts(0, 0, 0, 0));
        LotOccupancy lot = recalculation.lot();
        return new ZoneOccupancyChange(
                lot.getParkingLotId(),
                space.getZoneId(),
                zone,
                lot.getCounts(),
                lot.isLowAvailability(),
                lot.getConsolidatedAt(),
                new SpaceChange(space.getSpaceId(), space.getStatus(), space.getUnknownReason(),
                        space.getStatusSince()),
                lot.getVersion()
        );
    }

    /**
     * An event that can never apply (unknown parking lot, another institution, a space or access
     * outside the layout) is recorded as handled and dropped, so it is not retried forever.
     */
    private static void drop(RecordSensorEventCommand command, String reason) {
        LOGGER.warn("Dropped sensing event {} of parking lot {}: {}.",
                command.eventId(), command.parkingLotId(), reason);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException exception) {
                    LOGGER.warn("Could not deliver an occupancy change to the subscribed clients.", exception);
                }
            }
        });
    }

    @Override
    @Transactional
    public OccupancySnapshot handle(CreateOccupancySnapshotCommand command) {
        Long parkingLotId = layouts.readableParkingLotOfZone(command.zoneId())
                .orElseThrow(() -> new OccupancyNotFoundException("Zone not found."));
        state.fresh(parkingLotId);
        return snapshots.save(OccupancySnapshot.create(
                parkingLotId,
                command.zoneId(),
                now(),
                spaces.countByZoneId(command.zoneId())
        ));
    }

    @Override
    @Transactional
    public List<OccupancySnapshot> handle(CreateParkingLotSnapshotCommand command) {
        Long parkingLotId = layouts.readableParkingLot(command.parkingLotId())
                .orElseThrow(() -> new OccupancyNotFoundException("Parking lot not found."));
        state.fresh(parkingLotId);
        Instant takenAt = now();
        return spaces.countByZone(parkingLotId).stream()
                .map(zone -> snapshots.save(OccupancySnapshot.create(
                        parkingLotId,
                        zone.zoneId(),
                        takenAt,
                        zone.counts()
                )))
                .toList();
    }

    @Override
    @Transactional
    public int recordMinuteSnapshots() {
        Instant now = clock.instant();
        state.refreshStaleLayouts(lots.findAllIds());
        return snapshots.recordAllZones(now.truncatedTo(ChronoUnit.MINUTES));
    }

    @Override
    @Transactional
    public int purgeExpiredHistory() {
        Instant now = clock.instant();
        return snapshots.deleteTakenBefore(now.minus(SNAPSHOT_RETENTION))
                + passages.deletePassedBefore(now.minus(PASSAGE_RETENTION))
                + sensorEvents.deleteReceivedBefore(now.minus(PASSAGE_RETENTION));
    }

    /** PostgreSQL keeps microseconds, so the returned time matches the stored one. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}

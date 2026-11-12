package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Access;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Level;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Space;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.Zone;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.*;

import org.springframework.stereotype.Component;

@Component
public class ParkingConfigurationJpaMapper {

    public ParkingLot toDomain(ParkingLotJpaEntity entity) {
        return ParkingLot.restore(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getCreatedBy(),
                entity.getName(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getOpensAt(),
                entity.getClosesAt(),
                entity.getSaturationThresholdPct(),
                entity.getLowBatteryThresholdPct(),
                entity.getStaleAfterSeconds(),
                entity.getFlowWindowMinutes(),
                entity.getAccuracyMarginPct(),
                entity.getOccupiedDebounceS(),
                entity.getFreedDebounceS(),
                entity.getLayoutVersion(),
                entity.getLayoutPublishedAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ParkingLotJpaEntity toJpa(ParkingLot value) {
        var entity = new ParkingLotJpaEntity();

        entity.setId(value.getId());
        entity.setInstitutionId(value.getInstitutionId());
        entity.setCreatedBy(value.getCreatedBy());
        entity.setName(value.getName());
        entity.setAddress(value.getAddress());
        entity.setLatitude(value.getLatitude());
        entity.setLongitude(value.getLongitude());
        entity.setOpensAt(value.getOpensAt());
        entity.setClosesAt(value.getClosesAt());

        entity.setSaturationThresholdPct(
                (short) value.getSaturationThresholdPct()
        );
        entity.setLowBatteryThresholdPct(
                (short) value.getLowBatteryThresholdPct()
        );
        entity.setStaleAfterSeconds(value.getStaleAfterSeconds());
        entity.setFlowWindowMinutes(
                (short) value.getFlowWindowMinutes()
        );
        entity.setAccuracyMarginPct(
                (short) value.getAccuracyMarginPct()
        );
        entity.setOccupiedDebounceS(
                (short) value.getOccupiedDebounceS()
        );
        entity.setFreedDebounceS(
                (short) value.getFreedDebounceS()
        );

        entity.setLayoutVersion(value.getLayoutVersion());
        entity.setLayoutPublishedAt(value.getLayoutPublishedAt());
        entity.setActive(value.isActive());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setUpdatedAt(value.getUpdatedAt());

        return entity;
    }

    public Zone toDomain(ZoneJpaEntity entity) {
        return Zone.restore(
                entity.getId(),
                entity.getParkingLotId(),
                entity.getName(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ZoneJpaEntity toJpa(Zone value) {
        var entity = new ZoneJpaEntity();

        entity.setId(value.getId());
        entity.setParkingLotId(value.getParkingLotId());
        entity.setName(value.getName());
        entity.setDescription(value.getDescription());
        entity.setSortOrder((short) value.getSortOrder());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setUpdatedAt(value.getUpdatedAt());

        return entity;
    }

    public Level toDomain(LevelJpaEntity entity) {
        return Level.restore(
                entity.getId(),
                entity.getZoneId(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LevelJpaEntity toJpa(Level value) {
        var entity = new LevelJpaEntity();

        entity.setId(value.getId());
        entity.setZoneId(value.getZoneId());
        entity.setName(value.getName());
        entity.setSortOrder((short) value.getSortOrder());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setUpdatedAt(value.getUpdatedAt());

        return entity;
    }

    public Space toDomain(SpaceJpaEntity entity) {
        return Space.restore(
                entity.getId(),
                entity.getLevelId(),
                entity.getLabel(),
                entity.getType(),
                entity.getPosX(),
                entity.getPosY(),
                entity.getStatus(),
                entity.getUnknownReason(),
                entity.getStatusSince(),
                entity.getLastEventAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SpaceJpaEntity toJpa(Space value) {
        var entity = new SpaceJpaEntity();

        entity.setId(value.getId());
        entity.setLevelId(value.getLevelId());
        entity.setLabel(value.getLabel());
        entity.setType(value.getType());
        entity.setPosX(value.getPosX());
        entity.setPosY(value.getPosY());
        entity.setStatus(value.getStatus());
        entity.setUnknownReason(value.getUnknownReason());
        entity.setStatusSince(value.getStatusSince());
        entity.setLastEventAt(value.getLastEventAt());
        entity.setActive(value.isActive());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setUpdatedAt(value.getUpdatedAt());

        return entity;
    }

    public Access toDomain(AccessJpaEntity entity) {
        return Access.restore(
                entity.getId(),
                entity.getParkingLotId(),
                entity.getName(),
                entity.getType(),
                entity.isFlowCountingEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AccessJpaEntity toJpa(Access value) {
        var entity = new AccessJpaEntity();

        entity.setId(value.getId());
        entity.setParkingLotId(value.getParkingLotId());
        entity.setName(value.getName());
        entity.setType(value.getType());
        entity.setFlowCountingEnabled(
                value.isFlowCountingEnabled()
        );
        entity.setCreatedAt(value.getCreatedAt());
        entity.setUpdatedAt(value.getUpdatedAt());

        return entity;
    }

    public CalendarEvent toDomain(
            CalendarEventJpaEntity entity
    ) {
        return CalendarEvent.restore(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getParkingLotId(),
                entity.getZoneId(),
                entity.getCreatedBy(),
                entity.getName(),
                new EventWindow(
                        entity.getStartAt(),
                        entity.getEndAt()
                ),
                entity.getType(),
                entity.getImpact(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CalendarEventJpaEntity toJpa(
            CalendarEvent value
    ) {
        var entity = new CalendarEventJpaEntity();

        entity.setId(value.getId());
        entity.setInstitutionId(value.getInstitutionId());
        entity.setParkingLotId(value.getParkingLotId());
        entity.setZoneId(value.getZoneId());
        entity.setCreatedBy(value.getCreatedBy());

        entity.setName(value.getName());
        entity.setType(value.getType());
        entity.setImpact(value.getImpact());

        entity.setStartAt(value.getWindow().startAt());
        entity.setEndAt(value.getWindow().endAt());

        entity.setCancelledAt(value.getCancelledAt());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setUpdatedAt(value.getUpdatedAt());

        return entity;
    }
}

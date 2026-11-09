package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers;

import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.ParkingLot;
import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.domain.model.entities.*;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.EventWindow;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.*;

import org.springframework.stereotype.Component;

@Component
public class ParkingConfigurationJpaMapper {

    public ParkingLot toDomain(ParkingLotJpaEntity entity) {
        return ParkingLot.restore(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getName()
        );
    }

    public ParkingLotJpaEntity toJpa(ParkingLot value) {
        var entity = new ParkingLotJpaEntity();

        entity.setId(value.getId());
        entity.setInstitutionId(value.getInstitutionId());
        entity.setName(value.getName());

        return entity;
    }

    public Zone toDomain(ZoneJpaEntity entity) {
        return Zone.restore(
                entity.getId(),
                entity.getParkingLotId(),
                entity.getName()
        );
    }

    public ZoneJpaEntity toJpa(Zone value) {
        var entity = new ZoneJpaEntity();

        entity.setId(value.getId());
        entity.setParkingLotId(value.getParkingLotId());
        entity.setName(value.getName());

        return entity;
    }

    public Level toDomain(LevelJpaEntity entity) {
        return Level.restore(
                entity.getId(),
                entity.getZoneId(),
                entity.getName()
        );
    }

    public LevelJpaEntity toJpa(Level value) {
        var entity = new LevelJpaEntity();

        entity.setId(value.getId());
        entity.setZoneId(value.getZoneId());
        entity.setName(value.getName());

        return entity;
    }

    public Space toDomain(SpaceJpaEntity entity) {
        return Space.restore(
                entity.getId(),
                entity.getLevelId(),
                entity.getLabel()
        );
    }

    public SpaceJpaEntity toJpa(Space value) {
        var entity = new SpaceJpaEntity();

        entity.setId(value.getId());
        entity.setLevelId(value.getLevelId());
        entity.setLabel(value.getLabel());

        return entity;
    }

    public Access toDomain(AccessJpaEntity entity) {
        return Access.restore(
                entity.getId(),
                entity.getParkingLotId(),
                entity.getName()
        );
    }

    public AccessJpaEntity toJpa(Access value) {
        var entity = new AccessJpaEntity();

        entity.setId(value.getId());
        entity.setParkingLotId(value.getParkingLotId());
        entity.setName(value.getName());

        return entity;
    }

    public CalendarEvent toDomain(CalendarEventJpaEntity entity) {
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
                )
        );
    }

    public CalendarEventJpaEntity toJpa(CalendarEvent value) {
        var entity = new CalendarEventJpaEntity();

        entity.setId(value.getId());
        entity.setInstitutionId(value.getInstitutionId());
        entity.setParkingLotId(value.getParkingLotId());
        entity.setZoneId(value.getZoneId());
        entity.setCreatedBy(value.getCreatedBy());
        entity.setName(value.getName());
        entity.setStartAt(value.getWindow().startAt());
        entity.setEndAt(value.getWindow().endAt());

        return entity;
    }
}

package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.mappers;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceJpaEntity;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceIncidentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ParkingSensingJpaMapper {

    public Device toDomain(DeviceJpaEntity entity) {
        return Device.restore(entity.getId(), entity.getInstitutionId(), entity.getParkingLotId(),
                entity.getHardwareId(), entity.getType(), entity.getReportIntervalSeconds(),
                entity.getStatus(), entity.getSpaceId(), entity.getAccessId(),
                entity.getLastSeenAt(), entity.getRegisteredAt());
    }

    public DeviceJpaEntity toJpa(Device device) {
        var entity = new DeviceJpaEntity();
        entity.setId(device.getId());
        entity.setInstitutionId(device.getInstitutionId());
        entity.setParkingLotId(device.getParkingLotId());
        entity.setHardwareId(device.getHardwareId());
        entity.setType(device.getType());
        entity.setReportIntervalSeconds(device.getReportIntervalSeconds());
        entity.setStatus(device.getStatus());
        entity.setSpaceId(device.getSpaceId());
        entity.setAccessId(device.getAccessId());
        entity.setLastSeenAt(device.getLastSeenAt());
        entity.setRegisteredAt(device.getRegisteredAt());
        return entity;
    }

    public DeviceIncident toDomain(DeviceIncidentJpaEntity entity) {
        return DeviceIncident.restore(entity.getId(), entity.getInstitutionId(), entity.getParkingLotId(),
                entity.getDeviceId(), entity.getHardwareId(), entity.getType(), entity.getDetails(),
                entity.getOccurredAt(), entity.getResolvedAt());
    }

    public DeviceIncidentJpaEntity toJpa(DeviceIncident incident) {
        var entity = new DeviceIncidentJpaEntity();
        entity.setId(incident.getId());
        entity.setInstitutionId(incident.getInstitutionId());
        entity.setParkingLotId(incident.getParkingLotId());
        entity.setDeviceId(incident.getDeviceId());
        entity.setHardwareId(incident.getHardwareId());
        entity.setType(incident.getType());
        entity.setDetails(incident.getDetails());
        entity.setOccurredAt(incident.getOccurredAt());
        entity.setResolvedAt(incident.getResolvedAt());
        return entity;
    }
}
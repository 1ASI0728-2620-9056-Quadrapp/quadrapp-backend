package com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.mappers;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.entities.DeviceIncident;

import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceJpaEntity;
import com.upc.quadrapp.parkingsensing.infrastructure.persistence.jpa.entities.DeviceIncidentJpaEntity;

import org.springframework.stereotype.Component;

@Component
public class ParkingSensingJpaMapper {

    public Device toDomain(DeviceJpaEntity entity) {
        return Device.restore(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getParkingLotId(),
                entity.getHardwareId(),
                entity.getType(),
                entity.getReportIntervalSeconds(),
                entity.getStatus(),
                entity.getGatewayId(),
                entity.getSpaceId(),
                entity.getAccessId(),
                entity.getReplacedBy(),
                entity.getRegisteredBy(),
                entity.getApiKeyHash(),
                entity.getDirectional(),
                entity.getBatteryPct() == null
                        ? null
                        : entity.getBatteryPct().intValue(),
                entity.getFirmwareVersion(),
                entity.getLastSeenAt(),
                entity.getDecommissionedAt(),
                entity.getRegisteredAt(),
                entity.getUpdatedAt()
        );
    }

    public DeviceJpaEntity toJpa(Device device) {
        var entity = new DeviceJpaEntity();

        entity.setId(device.getId());
        entity.setParkingLotId(device.getParkingLotId());
        entity.setHardwareId(device.getHardwareId());
        entity.setType(device.getType());
        entity.setReportIntervalSeconds(
                device.getReportIntervalSeconds()
        );
        entity.setStatus(device.getStatus());

        entity.setGatewayId(device.getGatewayId());
        entity.setSpaceId(device.getSpaceId());
        entity.setAccessId(device.getAccessId());
        entity.setReplacedBy(device.getReplacedBy());
        entity.setRegisteredBy(device.getRegisteredBy());

        entity.setApiKeyHash(device.getApiKeyHash());
        entity.setDirectional(device.getDirectional());
        entity.setBatteryPct(
                device.getBatteryPct() == null
                        ? null
                        : device.getBatteryPct().shortValue()
        );
        entity.setFirmwareVersion(device.getFirmwareVersion());

        entity.setLastSeenAt(device.getLastSeenAt());
        entity.setDecommissionedAt(
                device.getDecommissionedAt()
        );
        entity.setRegisteredAt(device.getRegisteredAt());
        entity.setUpdatedAt(device.getUpdatedAt());

        return entity;
    }

    public DeviceIncident toDomain(
            DeviceIncidentJpaEntity entity
    ) {
        return DeviceIncident.restore(
                entity.getId(),
                entity.getInstitutionId(),
                entity.getParkingLotId(),
                entity.getDeviceId(),
                entity.getHardwareId(),
                entity.getType(),
                entity.getCategory(),
                entity.getReportedBy(),
                entity.getResolvedBy(),
                entity.getDetails(),
                entity.getOccurredAt(),
                entity.getResolvedAt()
        );
    }

    public DeviceIncidentJpaEntity toJpa(
            DeviceIncident incident
    ) {
        var entity = new DeviceIncidentJpaEntity();

        entity.setId(incident.getId());
        entity.setParkingLotId(incident.getParkingLotId());
        entity.setDeviceId(incident.getDeviceId());
        entity.setHardwareId(incident.getHardwareId());

        entity.setType(incident.getType());
        entity.setCategory(incident.getCategory());

        entity.setReportedBy(incident.getReportedBy());
        entity.setResolvedBy(incident.getResolvedBy());
        entity.setDetails(incident.getDetails());

        entity.setOccurredAt(incident.getOccurredAt());
        entity.setResolvedAt(incident.getResolvedAt());

        return entity;
    }
}

package com.upc.quadrapp.parkingsensing.interfaces.acl;

import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDeviceByIdQuery;
import com.upc.quadrapp.parkingsensing.domain.model.queries.GetDevicesByParkingLotQuery;
import com.upc.quadrapp.parkingsensing.domain.services.DeviceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParkingSensingContextFacade {

    private final DeviceQueryService queries;

    public Optional<DeviceView> deviceOf(Long deviceId) {
        return queries.handle(new GetDeviceByIdQuery(deviceId))
                .map(device -> new DeviceView(
                        device.getId(),
                        device.getInstitutionId(),
                        device.getParkingLotId(),
                        device.getHardwareId(),
                        device.getType().name(),
                        device.getStatus().name(),
                        device.getSpaceId(),
                        device.getAccessId()
                ));
    }

    public List<DeviceView> devicesOf(Long institutionId, Long parkingLotId) {
        return queries.handle(new GetDevicesByParkingLotQuery(institutionId, parkingLotId))
                .stream()
                .map(device -> new DeviceView(
                        device.getId(),
                        device.getInstitutionId(),
                        device.getParkingLotId(),
                        device.getHardwareId(),
                        device.getType().name(),
                        device.getStatus().name(),
                        device.getSpaceId(),
                        device.getAccessId()
                ))
                .toList();
    }

    public record DeviceView(
            Long id,
            Long institutionId,
            Long parkingLotId,
            String hardwareId,
            String type,
            String status,
            Long spaceId,
            Long accessId
    ) {}
}
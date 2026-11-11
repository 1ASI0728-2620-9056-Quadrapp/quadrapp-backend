package com.upc.quadrapp.parkingsensing.domain.services;

import com.upc.quadrapp.parkingsensing.domain.model.aggregates.Device;
import com.upc.quadrapp.parkingsensing.domain.model.queries.*;
import java.util.List;
import java.util.Optional;

public interface DeviceQueryService {
    Optional<Device> handle(GetDeviceByIdQuery query);
    Optional<Device> handle(GetDeviceByHardwareIdQuery query);
    List<Device> handle(GetDevicesByParkingLotQuery query);
}

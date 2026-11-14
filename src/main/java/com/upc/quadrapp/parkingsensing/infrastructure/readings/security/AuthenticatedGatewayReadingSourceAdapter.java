package com.upc.quadrapp.parkingsensing.infrastructure.readings.security;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.persistence.DevicePersistencePort;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.TrustedReadingSourcePort;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedGatewayReadingSourceAdapter implements TrustedReadingSourcePort {
    private final DevicePersistencePort devices;

    @Override
    public void requireTrustedSource(Long institutionId, SensorReading reading) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof VerifiedGatewayPrincipal gateway)) {
            throw new AccessDeniedException("Authenticated MQTT gateway is required.");
        }
        if (!gateway.institutionId().equals(institutionId)
                || reading.gatewayHardwareId() == null
                || !gateway.gatewayHardwareId().equals(reading.gatewayHardwareId())) {
            throw new AccessDeniedException("Gateway identity does not match the reading.");
        }
        var gatewayDevice = devices.findByHardwareId(gateway.gatewayHardwareId())
                .orElseThrow(() -> new AccessDeniedException("Gateway not registered."));
        if (!gatewayDevice.getInstitutionId().equals(institutionId)
                || gatewayDevice.getType() != DeviceType.GATEWAY
                || gatewayDevice.getStatus() == DeviceStatus.DECOMMISSIONED) {
            throw new AccessDeniedException("Gateway is not active for this institution.");
        }
        // Every sensor reports through its own gateway (CON-15, D7a): a gateway may only send
        // its own readings and those of the sensors linked to it. An unknown sensor is rejected
        // later as an unknown device.
        if (!reading.hardwareId().equals(gateway.gatewayHardwareId())) {
            devices.findByHardwareId(reading.hardwareId())
                    .filter(sensor -> !gatewayDevice.getId().equals(sensor.getGatewayId()))
                    .ifPresent(sensor -> {
                        throw new AccessDeniedException("Sensor is not linked to this gateway.");
                    });
        }
    }
}
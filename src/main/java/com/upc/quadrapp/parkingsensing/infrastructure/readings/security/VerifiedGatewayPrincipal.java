package com.upc.quadrapp.parkingsensing.infrastructure.readings.security;

import java.security.Principal;

public record VerifiedGatewayPrincipal(Long institutionId, String gatewayHardwareId)
        implements Principal {
    public VerifiedGatewayPrincipal {
        if (institutionId == null || institutionId <= 0) {
            throw new IllegalArgumentException("institutionId must be positive.");
        }
        if (gatewayHardwareId == null || gatewayHardwareId.isBlank()) {
            throw new IllegalArgumentException("gatewayHardwareId is required.");
        }
    }
    @Override public String getName() { return gatewayHardwareId; }
}
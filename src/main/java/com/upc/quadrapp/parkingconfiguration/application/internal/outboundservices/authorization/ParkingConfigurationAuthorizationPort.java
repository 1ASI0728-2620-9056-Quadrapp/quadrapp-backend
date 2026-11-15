package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization;

public interface ParkingConfigurationAuthorizationPort {

    void requireInstitutionRead(Long institutionId);

    /** Whether the caller can see the institution's data; other institutions look missing. */
    boolean canRead(Long institutionId);

    void requireInstitutionWrite(Long institutionId);
}

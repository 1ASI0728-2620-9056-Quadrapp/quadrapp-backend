package com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization;

public interface ParkingConfigurationAuthorizationPort {

    void requireInstitutionRead(Long institutionId);

    void requireInstitutionWrite(Long institutionId);
}

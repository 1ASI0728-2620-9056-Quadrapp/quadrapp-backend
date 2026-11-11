package com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization;

public interface ParkingSensingAuthorizationPort {
    void requireInstitutionRead(Long institutionId);
    void requireInstitutionWrite(Long institutionId);
    void requireIncidentResolver(Long actorUserId, Long institutionId);
}
package com.upc.quadrapp.parkingsensing.infrastructure.authorization.acl;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.authorization.ParkingSensingAuthorizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IamParkingSensingAuthorizationAdapter implements ParkingSensingAuthorizationPort {
    private final IamContextFacade iam;

    @Override
    public void requireInstitutionRead(Long institutionId) {
        iam.requireInstitutionAccess(institutionId);
    }

    @Override
    public void requireInstitutionWrite(Long institutionId) {
        iam.requireInstitutionAdmin(institutionId);
    }

    @Override
    public void requireIncidentResolver(Long actorUserId, Long institutionId) {
        if (!iam.getCurrentUser().userId().equals(actorUserId)) {
            throw new AccessDeniedException("Incident resolver must match authenticated user.");
        }
        iam.requireInstitutionAdmin(institutionId);
    }
}
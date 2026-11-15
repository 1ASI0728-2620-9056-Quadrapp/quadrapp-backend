package com.upc.quadrapp.parkingconfiguration.infrastructure.authorization.acl;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.authorization.ParkingConfigurationAuthorizationPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IamParkingAuthorizationAdapter
        implements ParkingConfigurationAuthorizationPort {

    private final IamContextFacade iamContextFacade;

    @Override
    public void requireInstitutionRead(Long institutionId) {
        iamContextFacade.requireInstitutionAccess(
                institutionId
        );
    }

    @Override
    public boolean canRead(Long institutionId) {
        return iamContextFacade.canAccessInstitution(
                institutionId
        );
    }

    @Override
    public void requireInstitutionWrite(Long institutionId) {
        iamContextFacade.requireInstitutionAdmin(
                institutionId
        );
    }
}

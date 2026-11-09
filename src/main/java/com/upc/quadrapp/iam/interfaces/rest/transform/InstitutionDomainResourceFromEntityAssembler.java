package com.upc.quadrapp.iam.interfaces.rest.transform;

import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.interfaces.rest.resources.InstitutionDomainResource;

public final class InstitutionDomainResourceFromEntityAssembler {

    private InstitutionDomainResourceFromEntityAssembler() {}

    public static InstitutionDomainResource toResourceFromEntity(
            InstitutionDomain domain
    ) {
        return new InstitutionDomainResource(
                domain.getId(),
                domain.getInstitutionId(),
                domain.getDomain().value(),
                domain.isVerified(),
                domain.getVerifiedBy(),
                domain.getVerifiedAt(),
                domain.getCreatedAt()
        );
    }
}

package com.upc.quadrapp.iam.interfaces.rest.transform;

import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.interfaces.rest.resources.InstitutionResource;

public final class InstitutionResourceFromEntityAssembler {

    private InstitutionResourceFromEntityAssembler() {}

    public static InstitutionResource toResourceFromEntity(
            Institution institution
    ) {
        return new InstitutionResource(
                institution.getId(),
                institution.getName(),
                institution.isActive(),
                institution.getCreatedAt(),
                institution.getUpdatedAt()
        );
    }
}

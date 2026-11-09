package com.upc.quadrapp.iam.application.internal.queryservices;

import com.upc.quadrapp.iam.application.internal.commandservices.IamAuthorizationService;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionDomainPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionPersistencePort;
import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.domain.model.queries.GetInstitutionByIdQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetInstitutionDomainsQuery;
import com.upc.quadrapp.iam.domain.services.InstitutionQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InstitutionQueryServiceImpl
        implements InstitutionQueryService {

    private final InstitutionPersistencePort institutionPersistencePort;
    private final InstitutionDomainPersistencePort institutionDomainPersistencePort;
    private final IamAuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public Optional<Institution> handle(
            GetInstitutionByIdQuery query
    ) {
        var actor = authorizationService.requireAuthenticatedUser();

        authorizationService.requireInstitutionAccess(
                actor,
                query.institutionId()
        );

        return institutionPersistencePort.findById(
                query.institutionId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstitutionDomain> handle(
            GetInstitutionDomainsQuery query
    ) {
        var actor = authorizationService.requireAuthenticatedUser();

        authorizationService.requireInstitutionAdmin(
                actor,
                query.institutionId()
        );

        return institutionDomainPersistencePort.findByInstitutionId(
                query.institutionId()
        );
    }
}


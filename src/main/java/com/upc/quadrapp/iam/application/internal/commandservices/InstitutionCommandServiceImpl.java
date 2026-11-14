package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionDomainPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionPersistencePort;
import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.domain.model.commands.AddInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.model.commands.CreateInstitutionCommand;
import com.upc.quadrapp.iam.domain.model.commands.VerifyInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.domain.model.valueobjects.InstitutionDomainName;
import com.upc.quadrapp.iam.domain.services.InstitutionCommandService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InstitutionCommandServiceImpl
        implements InstitutionCommandService {

    private final InstitutionPersistencePort institutionPersistencePort;
    private final InstitutionDomainPersistencePort institutionDomainPersistencePort;
    private final IamAuthorizationService authorizationService;

    @Override
    @Transactional
    public Institution handle(CreateInstitutionCommand command) {
        var actor = authorizationService.requireActor(
                command.requestedByUserId()
        );

        authorizationService.requirePlatformAdmin(actor);

        Institution institution = Institution.create(
                command.name(),
                Instant.now()
        );

        if (institutionPersistencePort.existsByName(
                institution.getName()
        )) {
            throw new IllegalStateException(
                    "Institution name is already registered."
            );
        }

        return institutionPersistencePort.save(institution);
    }

    @Override
    @Transactional
    public InstitutionDomain handle(
            AddInstitutionDomainCommand command
    ) {
        var actor = authorizationService.requireActor(
                command.requestedByUserId()
        );

        authorizationService.requireInstitutionAdmin(
                actor,
                command.institutionId()
        );

        Institution institution = institutionPersistencePort
                .findById(command.institutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Institution not found."
                ));

        if (!institution.isActive()) {
            throw new IllegalStateException(
                    "Institution is not active."
            );
        }

        InstitutionDomainName domainName =
                new InstitutionDomainName(command.domain());

        if (institutionDomainPersistencePort.existsByInstitutionIdAndDomain(
                institution.getId(), domainName.value())
                || institutionDomainPersistencePort
                .findVerifiedByDomain(domainName.value())
                .isPresent()) {
            throw new IllegalStateException(
                    "Institution domain is already registered."
            );
        }

        InstitutionDomain institutionDomain =
                InstitutionDomain.create(
                        institution.getId(),
                        domainName.value(),
                        Instant.now()
                );

        return institutionDomainPersistencePort.save(
                institutionDomain
        );
    }

    @Override
    @Transactional
    public InstitutionDomain handle(
            VerifyInstitutionDomainCommand command
    ) {
        var actor = authorizationService.requireActor(
                command.verifiedByUserId()
        );

        authorizationService.requirePlatformAdmin(actor);

        InstitutionDomain institutionDomain =
                institutionDomainPersistencePort
                        .findById(command.institutionDomainId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Institution domain not found."
                        ));

        Institution institution = institutionPersistencePort
                .findById(institutionDomain.getInstitutionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Institution was not found."
                ));

        if (!institution.isActive()) {
            throw new IllegalStateException(
                    "Cannot verify a domain of an inactive institution."
            );
        }

        boolean verifiedElsewhere = institutionDomainPersistencePort
                .findVerifiedByDomain(institutionDomain.getDomain().value())
                .filter(verified -> !verified.getId().equals(institutionDomain.getId()))
                .isPresent();

        if (verifiedElsewhere) {
            throw new IllegalStateException(
                    "Institution domain is already verified by another institution."
            );
        }

        institutionDomain.verify(
                actor.getId(),
                Instant.now()
        );

        return institutionDomainPersistencePort.save(
                institutionDomain
        );
    }
}


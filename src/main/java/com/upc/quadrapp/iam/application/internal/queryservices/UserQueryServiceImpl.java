package com.upc.quadrapp.iam.application.internal.queryservices;

import com.upc.quadrapp.iam.application.internal.commandservices.IamAuthorizationService;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.UserPersistencePort;
import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByEmailQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUsersByInstitutionQuery;
import com.upc.quadrapp.iam.domain.model.valueobjects.EmailAddress;
import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;
import com.upc.quadrapp.iam.domain.services.UserQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserPersistencePort userPersistencePort;
    private final IamAuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> handle(GetUserByIdQuery query) {
        var actor = authorizationService.requireAuthenticatedUser();

        Optional<User> user = userPersistencePort.findById(
                query.userId()
        );

        // A user the actor cannot see looks missing, so its existence is not revealed.
        return user.filter(target ->
                authorizationService.canAccessUser(actor, target)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> handle(GetUserByEmailQuery query) {
        var actor = authorizationService.requireAuthenticatedUser();
        EmailAddress email = new EmailAddress(query.email());

        boolean ownEmail = actor.getEmail().equals(email);

        boolean isAdministrator =
                actor.getRole() == Roles.ADMIN ||
                        actor.getRole() == Roles.PLATFORM_ADMIN;

        if (!ownEmail && !isAdministrator) {
            throw new AccessDeniedException(
                    "Permission to search users is required."
            );
        }

        Optional<User> user = userPersistencePort.findByEmail(
                email.value()
        );

        // A user the actor cannot see looks missing, so its existence is not revealed.
        return user.filter(target ->
                authorizationService.canAccessUser(actor, target)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> handle(
            GetUsersByInstitutionQuery query
    ) {
        var actor = authorizationService.requireAuthenticatedUser();

        authorizationService.requireInstitutionAdmin(
                actor,
                query.institutionId()
        );

        return userPersistencePort.findByInstitutionId(
                query.institutionId()
        );
    }
}


package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.UserPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;
import com.upc.quadrapp.iam.domain.model.valueobjects.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IamAuthorizationService {

    private final CurrentUserService currentUserService;
    private final UserPersistencePort userPersistencePort;
    private final InstitutionPersistencePort institutionPersistencePort;

    public User requireAuthenticatedUser() {
        Long userId = currentUserService.requireAuthenticatedUserId();

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user was not found."
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Authenticated user is not active."
            );
        }

        if (user.getRole() != Roles.PLATFORM_ADMIN) {
            var institution = institutionPersistencePort
                    .findById(user.getInstitutionId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "User institution was not found."
                    ));

            if (!institution.isActive()) {
                throw new AccessDeniedException(
                        "User institution is not active."
                );
            }
        }

        return user;
    }

    public User requireActor(Long requestedByUserId) {
        User actor = requireAuthenticatedUser();

        if (!Objects.equals(actor.getId(), requestedByUserId)) {
            throw new AccessDeniedException(
                    "Requester does not match authenticated user."
            );
        }

        return actor;
    }

    public void requirePlatformAdmin(User actor) {
        if (actor.getRole() != Roles.PLATFORM_ADMIN) {
            throw new AccessDeniedException(
                    "Platform administrator permission is required."
            );
        }
    }

    public void requireInstitutionAccess(
            User actor,
            Long institutionId
    ) {
        if (actor.getRole() == Roles.PLATFORM_ADMIN) {
            return;
        }

        if (!Objects.equals(
                actor.getInstitutionId(),
                institutionId
        )) {
            throw new AccessDeniedException(
                    "Access to another institution is not allowed."
            );
        }
    }

    public void requireInstitutionAdmin(
            User actor,
            Long institutionId
    ) {
        if (actor.getRole() == Roles.PLATFORM_ADMIN) {
            return;
        }

        boolean isAdmin = actor.getRole() == Roles.ADMIN;
        boolean sameInstitution = Objects.equals(
                actor.getInstitutionId(),
                institutionId
        );

        if (!isAdmin || !sameInstitution) {
            throw new AccessDeniedException(
                    "Institution administrator permission is required."
            );
        }
    }

    public void requireUserAccess(User actor, User target) {
        if (!canAccessUser(actor, target)) {
            throw new AccessDeniedException(
                    "Institution administrator permission is required."
            );
        }
    }

    /**
     * A user can see itself; an institution administrator sees the users of its institution;
     * a platform administrator sees everyone.
     */
    public boolean canAccessUser(User actor, User target) {
        if (actor.getRole() == Roles.PLATFORM_ADMIN) {
            return true;
        }

        if (Objects.equals(actor.getId(), target.getId())) {
            return true;
        }

        return actor.getRole() == Roles.ADMIN &&
                Objects.equals(actor.getInstitutionId(), target.getInstitutionId());
    }

    public void requireSelf(User actor, Long userId) {
        if (!Objects.equals(actor.getId(), userId)) {
            throw new AccessDeniedException(
                    "Users can only modify their own preferences."
            );
        }
    }
}


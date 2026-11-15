package com.upc.quadrapp.iam.interfaces.acl;

import com.upc.quadrapp.iam.application.internal.commandservices.IamAuthorizationService;
import com.upc.quadrapp.iam.domain.model.aggregates.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IamContextFacade {

    private final IamAuthorizationService authorizationService;

    public CurrentUserIdentity getCurrentUser() {

        User user = authorizationService.requireAuthenticatedUser();

        return new CurrentUserIdentity(
                user.getId(),
                user.getInstitutionId(),
                user.getRole().name(),
                user.getLocale()
        );
    }

    public void requireInstitutionAccess(Long institutionId) {

        User actor = authorizationService.requireAuthenticatedUser();

        authorizationService.requireInstitutionAccess(
                actor,
                institutionId
        );
    }

    /**
     * Whether the authenticated user can see data of the institution. Modules use it to answer
     * "not found" for another institution's resources instead of revealing that they exist.
     */
    public boolean canAccessInstitution(Long institutionId) {

        User actor = authorizationService.requireAuthenticatedUser();

        return authorizationService.canAccessInstitution(
                actor,
                institutionId
        );
    }

    public void requireInstitutionAdmin(Long institutionId) {

        User actor = authorizationService.requireAuthenticatedUser();

        authorizationService.requireInstitutionAdmin(
                actor,
                institutionId
        );
    }

    public record CurrentUserIdentity(
            Long userId,
            Long institutionId,
            String role,
            String locale
    ) {}
}

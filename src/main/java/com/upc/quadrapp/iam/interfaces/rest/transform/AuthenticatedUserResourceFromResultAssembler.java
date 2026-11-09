package com.upc.quadrapp.iam.interfaces.rest.transform;

import com.upc.quadrapp.iam.domain.services.AuthenticationResult;
import com.upc.quadrapp.iam.interfaces.rest.resources.AuthenticatedUserResource;

import java.time.Duration;
import java.time.Instant;

public final class AuthenticatedUserResourceFromResultAssembler {

    private AuthenticatedUserResourceFromResultAssembler() {}

    public static AuthenticatedUserResource toResourceFromResult(
            AuthenticationResult result
    ) {
        long expiresIn = Math.max(
                0,
                Duration.between(
                        Instant.now(),
                        result.accessTokenExpiresAt()
                ).toSeconds()
        );

        return new AuthenticatedUserResource(
                UserResourceFromEntityAssembler.toResourceFromEntity(
                        result.user()
                ),
                result.accessToken(),
                result.refreshToken(),
                expiresIn,
                result.accessTokenExpiresAt()
        );
    }
}

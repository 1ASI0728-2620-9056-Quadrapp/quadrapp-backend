package com.upc.quadrapp.iam.infrastructure.authorization.sfs.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SpringCurrentUserService implements CurrentUserService {

    @Override
    public Long requireAuthenticatedUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Valid authentication is required.");
        }

        try {
            return Long.parseLong(jwt.getToken().getSubject());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new AccessDeniedException("Invalid JWT subject.", ex);
        }
    }
}

package com.upc.quadrapp.iam.application.internal.outboundservices.security;

public interface CurrentUserService {
    Long requireAuthenticatedUserId();
}

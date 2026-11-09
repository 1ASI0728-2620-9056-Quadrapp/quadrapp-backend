package com.upc.quadrapp.iam.infrastructure.tokens.jwt.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRandomTokenService implements SecureTokenGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {

        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

package com.upc.quadrapp.iam.infrastructure.tokens.jwt.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.tokens.AccessTokenService;
import com.upc.quadrapp.iam.application.internal.outboundservices.tokens.IssuedAccessToken;
import com.upc.quadrapp.iam.domain.model.aggregates.User;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAccessTokenService implements AccessTokenService {
    private final JwtEncoder encoder;

    @Value("${quadrapp.security.jwt.access-expiration-seconds:900}")
    private long expirationSeconds;

    @Value("${quadrapp.security.jwt.issuer:quadrapp}")
    private String issuer;

    @Override
    public IssuedAccessToken issue(
            User user,
            Instant issuedAt
    ) {
        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "User must be persisted."
            );
        }

        if (expirationSeconds <= 0) {
            throw new IllegalStateException(
                    "Invalid JWT expiration."
            );
        }

        Instant expiresAt = issuedAt.plusSeconds(
                expirationSeconds
        );


        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail().value())
                .claim("role", user.getRole().name());

        if (user.getInstitutionId() != null) {
            builder.claim(
                    "institutionId",
                    user.getInstitutionId()
            );
        }

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = encoder.encode(
                JwtEncoderParameters.from(
                        header,
                        builder.build()
                )
        ).getTokenValue();

        return new IssuedAccessToken(
                token,
                expiresAt
        );
    }
}

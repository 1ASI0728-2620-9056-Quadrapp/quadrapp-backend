package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.hashing.TokenHashingService;
import com.upc.quadrapp.iam.domain.model.commands.RefreshSessionCommand;
import com.upc.quadrapp.iam.domain.services.AuthenticationCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Refresh token rotation: presenting a token that was already rotated means it was copied, so
 * the whole session it belongs to (its family) is revoked. Other sessions of the user stay.
 */
@QuadrappIntegrationTest
class RefreshTokenReuseIntegrationTest {

    @Autowired
    private AuthenticationCommandService authentication;

    @Autowired
    private TokenHashingService hashing;

    @Autowired
    private JdbcTemplate jdbc;

    private long userId;

    @BeforeEach
    void activeDriver() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long institutionId = jdbc.queryForObject(
                "insert into iam.institutions (name) values (?) returning id", Long.class, "Reuse " + suffix);
        jdbc.update("insert into iam.institution_domains (institution_id, domain, verified_at) values (?, ?, now())",
                institutionId, suffix + ".reuse.test");
        userId = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                        + "values (?, 'DRIVER', ?, 'ACTIVE') returning id",
                Long.class, institutionId, "driver@" + suffix + ".reuse.test");
    }

    @Test
    void reusingARotatedTokenRevokesItsSessionButNotTheOthers() {
        String firstToken = "first-" + UUID.randomUUID();
        String otherDeviceToken = "other-" + UUID.randomUUID();
        storeToken(firstToken, UUID.randomUUID());
        storeToken(otherDeviceToken, UUID.randomUUID());

        String rotatedToken = authentication.handle(new RefreshSessionCommand(firstToken)).refreshToken();

        assertThatThrownBy(() -> authentication.handle(new RefreshSessionCommand(firstToken)))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authentication.handle(new RefreshSessionCommand(rotatedToken)))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(authentication.handle(new RefreshSessionCommand(otherDeviceToken)).refreshToken()).isNotBlank();
    }

    private void storeToken(String rawToken, UUID familyId) {
        jdbc.update("insert into iam.refresh_tokens (user_id, token_hash, expires_at, family_id) "
                        + "values (?, ?, now() + interval '1 day', ?)",
                userId, hashing.hash("REFRESH_TOKEN", rawToken), familyId);
    }
}

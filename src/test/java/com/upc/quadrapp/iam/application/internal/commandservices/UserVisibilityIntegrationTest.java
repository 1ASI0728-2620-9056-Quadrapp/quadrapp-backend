package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByEmailQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.quadrapp.iam.domain.services.UserQueryService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * An administrator cannot tell whether a user of another institution exists: the lookup answers
 * "not found" exactly as for an unknown user, instead of 403.
 */
@QuadrappIntegrationTest
class UserVisibilityIntegrationTest {

    @Autowired
    private UserQueryService users;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserService currentUser;

    private long otherUserId;
    private String otherEmail;
    private long ownUserId;

    @BeforeEach
    void twoInstitutions() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long ownInstitution = jdbc.queryForObject(
                "insert into iam.institutions (name) values (?) returning id", Long.class, "Own " + suffix);
        long otherInstitution = jdbc.queryForObject(
                "insert into iam.institutions (name) values (?) returning id", Long.class, "Other " + suffix);
        long admin = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'ADMIN', ?, 'ACTIVE') returning id", Long.class, ownInstitution, "admin@own-" + suffix + ".test");
        ownUserId = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'DRIVER', ?, 'ACTIVE') returning id", Long.class, ownInstitution, "driver@own-" + suffix + ".test");
        otherEmail = "driver@other-" + suffix + ".test";
        otherUserId = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'DRIVER', ?, 'ACTIVE') returning id", Long.class, otherInstitution, otherEmail);
        when(currentUser.requireAuthenticatedUserId()).thenReturn(admin);
    }

    @Test
    void usersOfAnotherInstitutionLookMissing() {
        assertThat(users.handle(new GetUserByIdQuery(otherUserId))).isEmpty();
        assertThat(users.handle(new GetUserByEmailQuery(otherEmail))).isEmpty();
    }

    @Test
    void usersOfTheOwnInstitutionAreStillFound() {
        assertThat(users.handle(new GetUserByIdQuery(ownUserId))).isPresent();
    }
}

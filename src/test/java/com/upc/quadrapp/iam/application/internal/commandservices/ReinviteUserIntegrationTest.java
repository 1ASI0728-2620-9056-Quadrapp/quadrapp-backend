package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.mail.InvitationDeliveryService;
import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.iam.domain.model.commands.InviteUserCommand;
import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;
import com.upc.quadrapp.iam.domain.services.UserCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An invitation that expired or never arrived can be sent again: inviting the same email
 * renews the token and its expiry instead of blocking the address forever.
 */
@QuadrappIntegrationTest
class ReinviteUserIntegrationTest {

    @Autowired
    private UserCommandService users;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private InvitationDeliveryService delivery;

    @MockitoBean
    private CurrentUserService currentUser;

    private long institutionId;
    private long adminId;
    private String domain;

    @BeforeEach
    void institutionWithAdmin() {
        domain = UUID.randomUUID().toString().substring(0, 8) + ".invite.test";
        institutionId = jdbc.queryForObject(
                "insert into iam.institutions (name) values (?) returning id", Long.class, "Invite " + domain);
        jdbc.update("insert into iam.institution_domains (institution_id, domain, verified_at) values (?, ?, now())",
                institutionId, domain);
        adminId = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'ADMIN', ?, 'ACTIVE') returning id", Long.class, institutionId, "admin@" + domain);
        when(currentUser.requireAuthenticatedUserId()).thenReturn(adminId);
    }

    @Test
    void invitingAPendingUserAgainRenewsTheInvitation() {
        String email = "ops@" + domain;
        long invitedId = users.handle(new InviteUserCommand(institutionId, adminId, email, Roles.OPERATOR)).getId();
        jdbc.update("update iam.users set invitation_expires_at = now() - interval '1 hour' where id = ?", invitedId);

        var reinvited = users.handle(new InviteUserCommand(institutionId, adminId, email, Roles.OPERATOR));

        assertThat(reinvited.getId()).isEqualTo(invitedId);
        assertThat(reinvited.getInvitationExpiresAt()).isAfter(Instant.now());
        ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
        verify(delivery, times(2)).send(eq(email), tokens.capture());
        assertThat(tokens.getAllValues().get(0)).isNotEqualTo(tokens.getAllValues().get(1));
        assertThat(jdbc.queryForObject("select invitation_expires_at from iam.users where id = ?",
                Timestamp.class, invitedId).toInstant()).isAfter(Instant.now());
    }

    @Test
    void invitingAnActiveUserIsStillAConflict() {
        assertThatThrownBy(() -> users.handle(
                new InviteUserCommand(institutionId, adminId, "admin@" + domain, Roles.OPERATOR)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email is already registered.");
    }
}

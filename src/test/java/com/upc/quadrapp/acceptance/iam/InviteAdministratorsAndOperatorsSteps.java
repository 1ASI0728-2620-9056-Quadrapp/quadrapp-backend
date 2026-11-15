package com.upc.quadrapp.acceptance.iam;

import com.upc.quadrapp.acceptance.AcceptanceSteps;
import com.upc.quadrapp.iam.application.internal.outboundservices.mail.InvitationDeliveryService;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US31: the administrator invites administrators and operators of the institution. The access
 * link carries the invitation token, which the outgoing mail service receives.
 */
public class InviteAdministratorsAndOperatorsSteps extends AcceptanceSteps {

    /** {@code quadrapp.iam.invitation-expiration-hours}. */
    private static final Duration INVITATION_VALIDITY = Duration.ofHours(48);

    @Autowired
    private InvitationDeliveryService invitationDelivery;

    private long institutionId;
    private long adminId;
    private String inviteeEmail;
    private String inviteeRole;
    private String invitationToken;
    private Instant sentAt;
    private ResultActions response;

    @Dado("que el administrador indica el correo y el rol de la persona invitada")
    public void theAdministratorGivesTheEmailAndRoleOfTheInvitee() {
        anAdministratorWithAVerifiedDomain();
        inviteeRole = "OPERATOR";
    }

    @Cuando("envía la invitación")
    public void sendsTheInvitation() throws Exception {
        sentAt = Instant.now();
        response = invite();
    }

    @Entonces("el sistema registra la invitación con su vigencia y envía el enlace de acceso a ese correo.")
    public void theInvitationIsRegisteredAndItsLinkSent() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(inviteeEmail))
                .andExpect(jsonPath("$.role").value(inviteeRole))
                .andExpect(jsonPath("$.institutionId").value(institutionId))
                .andExpect(jsonPath("$.status").value("INVITED"));
        Timestamp expiresAt = jdbc.queryForObject("select invitation_expires_at from iam.users where email = ?",
                Timestamp.class, inviteeEmail);
        assertThat(expiresAt.toInstant()).isCloseTo(sentAt.plus(INVITATION_VALIDITY), within(Duration.ofMinutes(1)));
        assertThat(sentToken()).isNotBlank();
    }

    @Dado("que la invitación superó su periodo de vigencia")
    public void theInvitationIsPastItsValidity() throws Exception {
        anAdministratorWithAVerifiedDomain();
        inviteeRole = "OPERATOR";
        invite().andExpect(status().isCreated());
        invitationToken = sentToken();
        jdbc.update("update iam.users set invitation_expires_at = now() - interval '1 minute' where email = ?",
                inviteeEmail);
    }

    @Cuando("la persona invitada intenta utilizarla")
    public void theInviteeTriesToUseIt() throws Exception {
        response = accept();
    }

    @Entonces("el sistema rechaza el acceso e informa que debe solicitar una nueva invitación.")
    public void theAccessIsRejectedAsExpired() throws Exception {
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invitation has expired."));
        assertThat(jdbc.queryForObject("select status from iam.users where email = ?", String.class, inviteeEmail))
                .isEqualTo("INVITED");
    }

    private void anAdministratorWithAVerifiedDomain() {
        institutionId = institution("Universidad");
        String domain = "uni-" + suffix + ".edu.pe";
        verifiedDomain(institutionId, domain);
        adminId = user(institutionId, "ADMIN", "admin@" + domain);
        inviteeEmail = "invitado@" + domain;
    }

    private ResultActions invite() throws Exception {
        return postAs(adminId, "/api/v1/iam/invitations", """
                {"institutionId": %d, "email": "%s", "role": "%s"}
                """.formatted(institutionId, inviteeEmail, inviteeRole));
    }

    private ResultActions accept() throws Exception {
        return postAnonymously("/api/v1/iam/invitation-acceptances", """
                {"invitationToken": "%s", "fullName": "Ana Torres"}
                """.formatted(invitationToken));
    }

    private String sentToken() {
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(invitationDelivery).send(eq(inviteeEmail), token.capture());
        return token.getValue();
    }
}

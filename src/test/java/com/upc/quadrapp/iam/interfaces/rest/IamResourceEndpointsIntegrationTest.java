package com.upc.quadrapp.iam.interfaces.rest;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The IAM endpoints are resources, not actions: accepting an invitation creates an
 * invitation acceptance, verifying a domain is a partial update of that domain inside its
 * institution, and users are found by filtering the users collection or by listing the users of
 * an institution. The former action-style paths no longer exist.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
class IamResourceEndpointsIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserCommandService users;

    @MockitoBean
    private InvitationDeliveryService delivery;

    @MockitoBean
    private CurrentUserService currentUser;

    private String suffix;
    private long institutionId;
    private long otherInstitutionId;
    private long adminId;
    private long platformAdminId;

    @BeforeEach
    void twoInstitutions() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        institutionId = institution("Own " + suffix);
        otherInstitutionId = institution("Other " + suffix);
        adminId = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'ADMIN', ?, 'ACTIVE') returning id", Long.class, institutionId, admin());
        platformAdminId = jdbc.queryForObject("insert into iam.users (role, email, status) "
                + "values ('PLATFORM_ADMIN', ?, 'ACTIVE') returning id", Long.class, "platform@" + suffix + ".test");
    }

    @Test
    void anInvitationIsAcceptedWithoutASessionByCreatingAnInvitationAcceptance() throws Exception {
        String domain = suffix + ".accept.test";
        jdbc.update("insert into iam.institution_domains (institution_id, domain, verified_at) values (?, ?, now())",
                institutionId, domain);
        when(currentUser.requireAuthenticatedUserId()).thenReturn(adminId);
        String email = "ops@" + domain;
        users.handle(new InviteUserCommand(institutionId, adminId, email, Roles.OPERATOR));
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(email), token.capture());
        String body = """
                {"invitationToken": "%s", "fullName": "Ana Torres"}
                """.formatted(token.getValue());

        mvc.perform(post("/api/v1/iam/invitations/accept").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/iam/invitation-acceptances")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Ana Torres"))
                .andExpect(jsonPath("$.institutionId").value(institutionId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void aPlatformAdminVerifiesADomainOfTheInstitutionWithAPartialUpdate() throws Exception {
        long domainId = domain(institutionId, suffix + ".own.test");
        long foreignDomainId = domain(otherInstitutionId, suffix + ".other.test");
        when(currentUser.requireAuthenticatedUserId()).thenReturn(platformAdminId);

        mvc.perform(post("/api/v1/iam/institutions/domains/{id}/verify", domainId).with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(patchStatus("/api/v1/iam/institutions/{institutionId}/domains/{domainId}", "UNVERIFIED",
                        institutionId, domainId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status: must be VERIFIED"));
        mvc.perform(patchStatus("/api/v1/iam/institutions/{institutionId}/domains/{domainId}", "VERIFIED",
                        institutionId, foreignDomainId))
                .andExpect(status().isNotFound());
        assertThat(verifiedAt(domainId)).isNull();
        assertThat(verifiedAt(foreignDomainId)).isNull();

        mvc.perform(patchStatus("/api/v1/iam/institutions/{institutionId}/domains/{domainId}", "VERIFIED",
                        institutionId, domainId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(domainId))
                .andExpect(jsonPath("$.institutionId").value(institutionId))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verifiedBy").value(platformAdminId));
        assertThat(verifiedAt(domainId)).isNotNull();
    }

    @Test
    void anInstitutionAdminCannotVerifyItsOwnDomain() throws Exception {
        long domainId = domain(institutionId, suffix + ".self.test");
        when(currentUser.requireAuthenticatedUserId()).thenReturn(adminId);

        mvc.perform(patchStatus("/api/v1/iam/institutions/{institutionId}/domains/{domainId}", "VERIFIED",
                        institutionId, domainId))
                .andExpect(status().isForbidden());
        assertThat(verifiedAt(domainId)).isNull();
    }

    @Test
    void aUserIsFoundByFilteringTheUsersCollectionByEmail() throws Exception {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(adminId);

        mvc.perform(get("/api/v1/iam/users/by-email").param("email", admin()).with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/iam/users/{id}", adminId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(admin()));
        mvc.perform(get("/api/v1/iam/users").param("email", admin()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.email").value(admin()));
        mvc.perform(get("/api/v1/iam/users").param("email", "nobody@" + suffix + ".test").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void theUsersOfAnInstitutionAreASubCollectionOfIt() throws Exception {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(adminId);

        mvc.perform(get("/api/v1/iam/users/institution/{id}", institutionId).with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/iam/institutions/{id}/users", institutionId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].email", hasItem(admin())));
        mvc.perform(get("/api/v1/iam/institutions/{id}/users", otherInstitutionId).with(jwt()))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder patchStatus(
            String path, String status, Object... ids) {
        return patch(path, ids).with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}");
    }

    private String admin() {
        return "admin@own-" + suffix + ".test";
    }

    private long institution(String name) {
        return jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id", Long.class, name);
    }

    private long domain(long institution, String domain) {
        return jdbc.queryForObject("insert into iam.institution_domains (institution_id, domain) values (?, ?) "
                + "returning id", Long.class, institution, domain);
    }

    private Timestamp verifiedAt(long domainId) {
        return jdbc.queryForObject("select verified_at from iam.institution_domains where id = ?",
                Timestamp.class, domainId);
    }
}

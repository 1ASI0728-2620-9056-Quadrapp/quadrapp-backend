package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.iam.domain.model.commands.AddInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.model.commands.VerifyInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.services.InstitutionCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Each institution admin manages its own email domains (report, administrators' user story on
 * domains). An unverified claim must not block the real owner: only a verified domain is unique.
 */
@QuadrappIntegrationTest
class InstitutionDomainClaimIntegrationTest {

    @Autowired
    private InstitutionCommandService institutions;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserService currentUser;

    private String domain;
    private long squatterInstitution;
    private long squatterAdmin;
    private long ownerInstitution;
    private long ownerAdmin;
    private long platformAdmin;

    @BeforeEach
    void twoInstitutions() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        domain = suffix + ".claim.test";
        squatterInstitution = institution("Squatter " + suffix);
        squatterAdmin = admin(squatterInstitution, "admin@squatter-" + suffix + ".test");
        ownerInstitution = institution("Owner " + suffix);
        ownerAdmin = admin(ownerInstitution, "admin@owner-" + suffix + ".test");
        platformAdmin = jdbc.queryForObject("insert into iam.users (role, email, status) "
                + "values ('PLATFORM_ADMIN', ?, 'ACTIVE') returning id", Long.class, "platform@" + suffix + ".test");
    }

    @Test
    void anUnverifiedClaimDoesNotBlockTheOwnerAndOnlyOneClaimCanBeVerified() {
        actAs(squatterAdmin);
        long squatterClaim = institutions.handle(
                new AddInstitutionDomainCommand(squatterInstitution, domain, squatterAdmin)).getId();

        actAs(ownerAdmin);
        long ownerClaim = institutions.handle(
                new AddInstitutionDomainCommand(ownerInstitution, domain, ownerAdmin)).getId();

        actAs(platformAdmin);
        assertThat(institutions.handle(new VerifyInstitutionDomainCommand(ownerClaim, platformAdmin)).isVerified())
                .isTrue();
        assertThatThrownBy(() -> institutions.handle(new VerifyInstitutionDomainCommand(squatterClaim, platformAdmin)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Institution domain is already verified by another institution.");
    }

    @Test
    void theSameInstitutionCannotAddADomainTwiceAndNobodyCanClaimAVerifiedOne() {
        actAs(ownerAdmin);
        long ownerClaim = institutions.handle(
                new AddInstitutionDomainCommand(ownerInstitution, domain, ownerAdmin)).getId();
        assertThatThrownBy(() -> institutions.handle(
                new AddInstitutionDomainCommand(ownerInstitution, domain, ownerAdmin)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Institution domain is already registered.");

        actAs(platformAdmin);
        institutions.handle(new VerifyInstitutionDomainCommand(ownerClaim, platformAdmin));

        actAs(squatterAdmin);
        assertThatThrownBy(() -> institutions.handle(
                new AddInstitutionDomainCommand(squatterInstitution, domain, squatterAdmin)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Institution domain is already registered.");
    }

    private long institution(String name) {
        return jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id", Long.class, name);
    }

    private long admin(long institutionId, String email) {
        return jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'ADMIN', ?, 'ACTIVE') returning id", Long.class, institutionId, email);
    }

    private void actAs(long userId) {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(userId);
    }
}

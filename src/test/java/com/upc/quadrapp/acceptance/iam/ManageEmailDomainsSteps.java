package com.upc.quadrapp.acceptance.iam;

import com.upc.quadrapp.acceptance.AcceptanceSteps;
import com.upc.quadrapp.iam.application.internal.outboundservices.mail.OtpDeliveryService;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US30: the administrator manages the email domains that may register in the institution. A
 * domain the administrator adds enables registration once a platform administrator verifies it.
 */
public class ManageEmailDomainsSteps extends AcceptanceSteps {

    @Autowired
    private OtpDeliveryService otpDelivery;

    private long institutionId;
    private long adminId;
    private String domain;
    private ResultActions response;

    @Dado("que el administrador pertenece a una institución registrada")
    public void theAdministratorBelongsToARegisteredInstitution() {
        institutionId = institution("Universidad");
        adminId = user(institutionId, "ADMIN", "admin@admin-" + suffix + ".test");
    }

    @Cuando("agrega un dominio de correo institucional")
    public void addsAnInstitutionalEmailDomain() throws Exception {
        domain = "uni-" + suffix + ".edu.pe";
        response = postAs(adminId, "/api/v1/iam/institutions/{institutionId}/domains", """
                {"domain": "%s"}
                """.formatted(domain), institutionId);

        // Ownership check of the platform: the domain only takes new users once it is verified.
        patchAs(platformAdmin(), "/api/v1/iam/institutions/{institutionId}/domains/{domainId}", """
                {"status": "VERIFIED"}
                """, institutionId, idOf(response))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Entonces("el sistema lo habilita para el registro de nuevos usuarios de esa institución.")
    public void theDomainIsEnabledForNewUsersOfTheInstitution() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.institutionId").value(institutionId))
                .andExpect(jsonPath("$.domain").value(domain));

        String email = "estudiante@" + domain;
        postAnonymously("/api/v1/iam/auth/request-otp", """
                {"email": "%s"}
                """.formatted(email))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(otpDelivery).send(eq(email), code.capture());

        postAnonymously("/api/v1/iam/auth/sign-in", """
                {"email": "%s", "otpCode": "%s"}
                """.formatted(email, code.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("DRIVER"))
                .andExpect(jsonPath("$.user.institutionId").value(institutionId))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Dado("que el dominio ya está habilitado para otra institución")
    public void theDomainIsAlreadyEnabledForAnotherInstitution() {
        theAdministratorBelongsToARegisteredInstitution();
        domain = "shared-" + suffix + ".edu.pe";
        verifiedDomain(institution("Otra Universidad"), domain);
    }

    @Cuando("el administrador intenta agregarlo")
    public void theAdministratorTriesToAddIt() throws Exception {
        response = postAs(adminId, "/api/v1/iam/institutions/{institutionId}/domains", """
                {"domain": "%s"}
                """.formatted(domain), institutionId);
    }

    @Entonces("el sistema rechaza la solicitud e informa el conflicto.")
    public void theRequestIsRejectedWithAConflict() throws Exception {
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Institution domain is already registered."));
        assertThat(jdbc.queryForObject("select count(*) from iam.institution_domains where institution_id = ?",
                Integer.class, institutionId)).isZero();
    }
}

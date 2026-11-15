package com.upc.quadrapp.acceptance;

import com.upc.quadrapp.iam.application.internal.outboundservices.mail.InvitationDeliveryService;
import com.upc.quadrapp.iam.application.internal.outboundservices.mail.OtpDeliveryService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The Spring context of the acceptance scenarios: the same application and PostgreSQL container
 * setup as the integration tests. Only the outgoing mail is replaced, so the steps can read the
 * invitation token and the access code that would have been emailed.
 */
@CucumberContextConfiguration
@QuadrappIntegrationTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {

    @MockitoBean
    private InvitationDeliveryService invitationDelivery;

    @MockitoBean
    private OtpDeliveryService otpDelivery;
}

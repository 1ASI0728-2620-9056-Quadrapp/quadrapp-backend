package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CON-09 scenario 2: after the requests allowed in the window, the same email gets 429 and no
 * code. The limit applies to every email, so a 429 does not reveal whether the email has access.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
class OtpRequestLimitIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sixthRequestInTheWindowIsRejectedWith429() throws Exception {
        String body = "{\"email\":\"nobody@unknown-domain.test\"}";
        for (int request = 0; request < 5; request++) {
            mvc.perform(post("/api/v1/iam/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isAccepted());
        }

        mvc.perform(post("/api/v1/iam/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many access code requests. Try again later."));
    }
}

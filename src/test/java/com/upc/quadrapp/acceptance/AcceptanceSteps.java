package com.upc.quadrapp.acceptance;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers of the step definitions. Cucumber creates the step classes again for every
 * scenario, so each scenario gets its own suffix and creates its own institution and users: no
 * scenario depends on data left by another one.
 * <p>
 * Requests go through the real security filter chain with a JWT whose subject is the acting
 * user, the same way the access token issued at sign-in identifies the user.
 */
public abstract class AcceptanceSteps {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    protected final String suffix = UUID.randomUUID().toString().substring(0, 8);

    protected ResultActions postAs(long userId, String path, String body, Object... uriVariables) throws Exception {
        return mvc.perform(post(path, uriVariables).with(as(userId))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions postAnonymously(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions patchAs(long userId, String path, String body, Object... uriVariables) throws Exception {
        return mvc.perform(patch(path, uriVariables).with(as(userId))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions getAs(long userId, String path, Object... uriVariables) throws Exception {
        return mvc.perform(get(path, uriVariables).with(as(userId)));
    }

    protected static long idOf(ResultActions response) throws Exception {
        return ((Number) read(response, "$.id")).longValue();
    }

    protected static <T> T read(ResultActions response, String jsonPath) throws Exception {
        return JsonPath.read(response.andReturn().getResponse().getContentAsString(), jsonPath);
    }

    protected long institution(String name) {
        return jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id",
                Long.class, name + " " + suffix);
    }

    protected long user(long institutionId, String role, String email) {
        return jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, ?, ?, 'ACTIVE') returning id", Long.class, institutionId, role, email);
    }

    protected long platformAdmin() {
        return jdbc.queryForObject("insert into iam.users (role, email, status) "
                + "values ('PLATFORM_ADMIN', ?, 'ACTIVE') returning id", Long.class, "platform@" + suffix + ".test");
    }

    protected long verifiedDomain(long institutionId, String domain) {
        return jdbc.queryForObject("insert into iam.institution_domains (institution_id, domain, verified_at) "
                + "values (?, ?, now()) returning id", Long.class, institutionId, domain);
    }

    /** Registers a parking lot through the API, as the given administrator. */
    protected long registerParkingLot(long adminId, String name) throws Exception {
        return idOf(postAs(adminId, "/api/v1/parking-lots", """
                {"name": "%s", "address": "Av. Universitaria 1801"}
                """.formatted(name)).andExpect(status().isCreated()));
    }

    private static RequestPostProcessor as(long userId) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)));
    }
}

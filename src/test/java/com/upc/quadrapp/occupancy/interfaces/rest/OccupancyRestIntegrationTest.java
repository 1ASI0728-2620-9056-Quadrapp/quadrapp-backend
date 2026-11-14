package com.upc.quadrapp.occupancy.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The occupancy endpoints keep their routes and response shape, and answer every resource of
 * another institution exactly like a missing one (404 with the same message), so IDs of other
 * tenants cannot be enumerated.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class OccupancyRestIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserService currentUser;

    private OccupancyTestLayout own;
    private OccupancyTestLayout other;

    @BeforeEach
    void twoInstitutions() {
        own = OccupancyTestLayout.create(jdbc, 3, 10);
        other = OccupancyTestLayout.create(jdbc, 2, 10);
        when(currentUser.requireAuthenticatedUserId()).thenReturn(own.userId);
    }

    @Test
    void ownParkingLotZoneAndLevelAnswerWithTheirCounts() throws Exception {
        mvc.perform(authenticated(get("/api/v1/parking-lots/{id}/occupancy", own.parkingLotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeType").value("PARKING_LOT"))
                .andExpect(jsonPath("$.scopeId").value(own.parkingLotId))
                .andExpect(jsonPath("$.totalSpaces").value(3))
                .andExpect(jsonPath("$.unknownCount").value(3))
                .andExpect(jsonPath("$.freeCount").value(0));
        mvc.perform(authenticated(get("/api/v1/zones/{id}/occupancy", own.zoneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeType").value("ZONE"))
                .andExpect(jsonPath("$.totalSpaces").value(3));
        mvc.perform(authenticated(get("/api/v1/levels/{id}/occupancy", own.levelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeType").value("LEVEL"))
                .andExpect(jsonPath("$.totalSpaces").value(3));
        mvc.perform(authenticated(post("/api/v1/zones/{id}/occupancy-snapshots", own.zoneId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zoneId").value(own.zoneId))
                .andExpect(jsonPath("$.totalSpaces").value(3));
        mvc.perform(authenticated(get("/api/v1/zones/{id}/occupancy-snapshots", own.zoneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void resourcesOfAnotherInstitutionAnswerLikeMissingOnes() throws Exception {
        expectNotFound(get("/api/v1/parking-lots/{id}/occupancy", other.parkingLotId), "Parking lot not found.");
        expectNotFound(get("/api/v1/parking-lots/{id}/occupancy", Long.MAX_VALUE), "Parking lot not found.");
        expectNotFound(get("/api/v1/zones/{id}/occupancy", other.zoneId), "Zone not found.");
        expectNotFound(get("/api/v1/zones/{id}/occupancy", Long.MAX_VALUE), "Zone not found.");
        expectNotFound(get("/api/v1/levels/{id}/occupancy", other.levelId), "Level not found.");
        expectNotFound(get("/api/v1/levels/{id}/occupancy", Long.MAX_VALUE), "Level not found.");
        expectNotFound(get("/api/v1/zones/{id}/occupancy-snapshots", other.zoneId), "Zone not found.");
        expectNotFound(post("/api/v1/zones/{id}/occupancy-snapshots", other.zoneId), "Zone not found.");
        expectNotFound(post("/api/v1/zones/{id}/occupancy-snapshots", Long.MAX_VALUE), "Zone not found.");
    }

    private void expectNotFound(MockHttpServletRequestBuilder request, String message) throws Exception {
        mvc.perform(authenticated(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt());
    }
}

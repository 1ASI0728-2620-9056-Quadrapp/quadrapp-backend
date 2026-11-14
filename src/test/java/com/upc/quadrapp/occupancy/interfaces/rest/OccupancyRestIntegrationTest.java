package com.upc.quadrapp.occupancy.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
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

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The occupancy endpoints keep their routes and response shape, and answer every resource of
 * another institution exactly like a missing one (404 with the same message), so IDs of other
 * tenants cannot be enumerated. Any user of the institution, drivers included, reads the state of
 * each space to overlay it on the layout.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class OccupancyRestIntegrationTest {

    private static final Instant DETECTED_AT = Instant.parse("2026-11-14T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OccupancyCommandService commands;

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
    void aDriverOfTheInstitutionReadsTheStateOfEachActiveSpace() throws Exception {
        long driver = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'DRIVER', ?, 'ACTIVE') returning id", Long.class, own.institutionId,
                "driver-" + UUID.randomUUID() + "@spaces.test");
        when(currentUser.requireAuthenticatedUserId()).thenReturn(driver);
        commands.handle(new RecordSensorEventCommand(own.institutionId, own.parkingLotId, 1L, own.space(1), null,
                UUID.randomUUID().toString(), OccupancyEventType.OCCUPIED, DETECTED_AT));

        mvc.perform(authenticated(get("/api/v1/parking-lots/{id}/spaces/status", own.parkingLotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].spaceId").value(own.space(0)))
                .andExpect(jsonPath("$[0].zoneId").value(own.zoneId))
                .andExpect(jsonPath("$[0].levelId").value(own.levelId))
                .andExpect(jsonPath("$[0].status").value("UNKNOWN"))
                .andExpect(jsonPath("$[0].unknownReason").value("NOT_YET_REPORTED"))
                .andExpect(jsonPath("$[0].lastDetectedAt").doesNotExist())
                .andExpect(jsonPath("$[1].spaceId").value(own.space(1)))
                .andExpect(jsonPath("$[1].status").value("OCCUPIED"))
                .andExpect(jsonPath("$[1].unknownReason").doesNotExist())
                .andExpect(jsonPath("$[1].statusSince").value(DETECTED_AT.toString()))
                .andExpect(jsonPath("$[1].lastDetectedAt").value(DETECTED_AT.toString()))
                .andExpect(jsonPath("$[*].spaceId", not(hasItem(own.inactiveSpaceId))));
    }

    @Test
    void resourcesOfAnotherInstitutionAnswerLikeMissingOnes() throws Exception {
        expectNotFound(get("/api/v1/parking-lots/{id}/occupancy", other.parkingLotId), "Parking lot not found.");
        expectNotFound(get("/api/v1/parking-lots/{id}/occupancy", Long.MAX_VALUE), "Parking lot not found.");
        expectNotFound(get("/api/v1/parking-lots/{id}/spaces/status", other.parkingLotId), "Parking lot not found.");
        expectNotFound(get("/api/v1/parking-lots/{id}/spaces/status", Long.MAX_VALUE), "Parking lot not found.");
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

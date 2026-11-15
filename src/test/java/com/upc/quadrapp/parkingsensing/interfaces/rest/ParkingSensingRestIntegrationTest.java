package com.upc.quadrapp.parkingsensing.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Devices and device incidents are handled as resources: a device is removed with DELETE (a
 * logical removal that keeps it as DECOMMISSIONED), an incident is resolved with a partial update
 * of its status, a device is found by filtering the devices collection by hardware id, and the
 * incidents collection is filtered by status. The former action-style paths no longer exist.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class ParkingSensingRestIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserService currentUser;

    private OccupancyTestLayout own;
    private OccupancyTestLayout other;
    private long ownAdmin;
    private long otherAdmin;
    private String suffix;

    @BeforeEach
    void twoInstitutions() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        own = OccupancyTestLayout.create(jdbc, 1, 10);
        other = OccupancyTestLayout.create(jdbc, 1, 10);
        ownAdmin = admin(own.institutionId);
        otherAdmin = admin(other.institutionId);
        actAs(ownAdmin);
    }

    @Test
    void aDeviceIsFoundByFilteringTheDevicesCollectionByHardwareId() throws Exception {
        long secondLot = parkingLot(own.institutionId);
        long id = register(own.parkingLotId, "GW-" + suffix);

        mvc.perform(get("/api/v1/devices/by-hardware/{hardwareId}", "GW-" + suffix).with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/devices").param("hardwareId", "GW-" + suffix).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].hardwareId").value("GW-" + suffix))
                .andExpect(jsonPath("$[0].parkingLotId").value(own.parkingLotId));
        mvc.perform(get("/api/v1/devices").param("hardwareId", "MISSING-" + suffix).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/devices").param("hardwareId", "GW-" + suffix)
                        .param("parkingLotId", String.valueOf(own.parkingLotId)).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/devices").param("hardwareId", "GW-" + suffix)
                        .param("parkingLotId", String.valueOf(secondLot)).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/devices").param("parkingLotId", String.valueOf(own.parkingLotId)).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/devices").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theHardwareIdFilterKeepsRejectingADeviceOfAnotherInstitution() throws Exception {
        actAs(otherAdmin);
        register(other.parkingLotId, "FOREIGN-" + suffix);
        actAs(ownAdmin);

        mvc.perform(get("/api/v1/devices").param("hardwareId", "FOREIGN-" + suffix).with(jwt()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/devices").param("hardwareId", "FOREIGN-" + suffix)
                        .param("parkingLotId", String.valueOf(other.parkingLotId)).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingADeviceDecommissionsItAndItCanStillBeRead() throws Exception {
        long id = register(own.parkingLotId, "GW-" + suffix);

        mvc.perform(post("/api/v1/devices/{id}/decommission", id).with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/devices/{id}", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("DECOMMISSIONED"))
                .andExpect(jsonPath("$.decommissionedAt").isNotEmpty());
        mvc.perform(get("/api/v1/devices/{id}", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECOMMISSIONED"));
        mvc.perform(delete("/api/v1/devices/{id}", Long.MAX_VALUE).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Device not found."));
    }

    @Test
    void anIncidentIsResolvedWithAPartialUpdateOfItsStatus() throws Exception {
        long device = register(own.parkingLotId, "GW-" + suffix);
        long incident = report(device);

        mvc.perform(post("/api/v1/device-incidents/{id}/resolve", incident).with(jwt()))
                .andExpect(status().isNotFound());
        mvc.perform(patchStatus("/api/v1/device-incidents/{id}", "OPEN", incident))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/device-incidents").param("status", "OPEN").with(jwt()))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(patchStatus("/api/v1/device-incidents/{id}", "RESOLVED", incident))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident))
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.resolvedBy").value(ownAdmin))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        mvc.perform(patchStatus("/api/v1/device-incidents/{id}", "RESOLVED", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident not found."));
    }

    @Test
    void theIncidentsCollectionIsFilteredByStatus() throws Exception {
        long device = register(own.parkingLotId, "GW-" + suffix);
        long resolved = report(device);
        mvc.perform(patchStatus("/api/v1/device-incidents/{id}", "RESOLVED", resolved))
                .andExpect(status().isOk());
        long open = report(device);

        mvc.perform(get("/api/v1/device-incidents").param("status", "OPEN").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(open));
        mvc.perform(get("/api/v1/device-incidents").param("status", "RESOLVED")
                        .param("parkingLotId", String.valueOf(own.parkingLotId)).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(resolved));
        mvc.perform(get("/api/v1/device-incidents").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/device-incidents").param("status", "CLOSED").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be OPEN or RESOLVED."));
        // The former boolean filter is no longer read: it leaves the collection unfiltered.
        mvc.perform(get("/api/v1/device-incidents").param("onlyOpen", "true").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private long register(long parkingLotId, String hardwareId) throws Exception {
        String body = """
                {"parkingLotId": %d, "hardwareId": "%s", "type": "GATEWAY", "reportIntervalSeconds": 60}
                """.formatted(parkingLotId, hardwareId);
        return idOf(mvc.perform(post("/api/v1/devices").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long report(long deviceId) throws Exception {
        String body = """
                {"parkingLotId": %d, "deviceId": %d, "category": "OTHER", "details": "Cable cut"}
                """.formatted(own.parkingLotId, deviceId);
        return idOf(mvc.perform(post("/api/v1/device-incidents").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder patchStatus(String path, String status, Object id) {
        return patch(path, id).with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}");
    }

    private long admin(long institutionId) {
        return jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'ADMIN', ?, 'ACTIVE') returning id", Long.class, institutionId,
                "admin-" + institutionId + "@" + UUID.randomUUID().toString().substring(0, 8) + ".test");
    }

    private long parkingLot(long institutionId) {
        return jdbc.queryForObject("insert into configuration.parking_lots (accuracy_margin_pct, active, "
                + "flow_window_minutes, freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name, "
                + "low_availability_pct) values (5, true, 15, 10, 1, 20, 10, 90, 300, now(), ?, now(), ?, 10) "
                + "returning id", Long.class, institutionId, "Second lot " + suffix);
    }

    private void actAs(long userId) {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(userId);
    }

    private static long idOf(String json) {
        return Long.parseLong(json.replaceAll("(?s)^\\{\\s*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

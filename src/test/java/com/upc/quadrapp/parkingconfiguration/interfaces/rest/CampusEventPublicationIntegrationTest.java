package com.upc.quadrapp.parkingconfiguration.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventCancelled;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventProvided;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registering a campus event publishes {@code CampusEventProvided} and cancelling it publishes
 * {@code CampusEventCancelled} (US35, scenario 3), which Prediction uses to regenerate forecasts.
 * Cancelling is {@code POST /api/v1/calendar-events/{id}/cancellation}: 200 with the cancelled
 * event, 409 when it was already cancelled, 404 for a missing event or one of another institution.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class CampusEventPublicationIntegrationTest {

    private static final Instant START = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
    private static final Instant END = START.plus(2, ChronoUnit.HOURS);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private CurrentUserService currentUser;

    private OccupancyTestLayout own;
    private OccupancyTestLayout other;
    private long ownAdmin;

    @BeforeEach
    void twoInstitutions() {
        own = OccupancyTestLayout.create(jdbc, 2, 10);
        other = OccupancyTestLayout.create(jdbc, 2, 10);
        ownAdmin = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'ADMIN', ?, 'ACTIVE') returning id", Long.class, own.institutionId,
                "admin@" + UUID.randomUUID().toString().substring(0, 8) + ".test");
        when(currentUser.requireAuthenticatedUserId()).thenReturn(ownAdmin);
    }

    @Test
    void schedulingAnEventPublishesCampusEventProvided() throws Exception {
        long id = schedule();

        assertThat(events.stream(CampusEventProvided.class))
                .containsExactly(new CampusEventProvided(id, own.institutionId, own.parkingLotId, START, END, "HIGH"));
    }

    @Test
    void cancellingAnEventMarksItAndPublishesCampusEventCancelledOnce() throws Exception {
        long id = schedule();

        mvc.perform(post("/api/v1/calendar-events/{id}/cancellation", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
        mvc.perform(post("/api/v1/calendar-events/{id}/cancellation", id).with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Calendar event is already cancelled."));

        assertThat(jdbc.queryForObject("select cancelled_at from configuration.calendar_events where id = ?",
                Timestamp.class, id)).isNotNull();
        assertThat(events.stream(CampusEventCancelled.class))
                .containsExactly(new CampusEventCancelled(id, own.institutionId, own.parkingLotId, START, END));
    }

    @Test
    void anEventOfAnotherInstitutionOrAMissingOneAnswers404() throws Exception {
        long foreign = jdbc.queryForObject("insert into configuration.calendar_events (institution_id, lot_id, "
                + "name, starts_at, ends_at, type, impact, created_at, updated_at) values (?, ?, 'Fair', ?, ?, "
                + "'CAMPUS_EVENT', 'HIGH', now(), now()) returning id", Long.class, other.institutionId,
                other.parkingLotId, Timestamp.from(START), Timestamp.from(END));

        mvc.perform(post("/api/v1/calendar-events/{id}/cancellation", foreign).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Calendar event not found."));
        mvc.perform(post("/api/v1/calendar-events/{id}/cancellation", Long.MAX_VALUE).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Calendar event not found."));

        assertThat(jdbc.queryForObject("select cancelled_at from configuration.calendar_events where id = ?",
                Timestamp.class, foreign)).isNull();
        assertThat(events.stream(CampusEventCancelled.class)).isEmpty();
    }

    @Test
    void anOperatorCannotCancel() throws Exception {
        long id = schedule();
        when(currentUser.requireAuthenticatedUserId()).thenReturn(own.userId);

        mvc.perform(post("/api/v1/calendar-events/{id}/cancellation", id).with(jwt()))
                .andExpect(status().isForbidden());
        assertThat(events.stream(CampusEventCancelled.class)).isEmpty();
    }

    private long schedule() throws Exception {
        String body = """
                {"parkingLotId": %d, "name": "Job fair", "startAt": "%s", "endAt": "%s",
                 "type": "CAMPUS_EVENT", "impact": "HIGH"}
                """.formatted(own.parkingLotId, START, END);
        String response = mvc.perform(post("/api/v1/calendar-events").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

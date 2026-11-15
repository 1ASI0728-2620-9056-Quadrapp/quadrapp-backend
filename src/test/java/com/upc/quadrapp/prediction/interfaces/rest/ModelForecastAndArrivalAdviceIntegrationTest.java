package com.upc.quadrapp.prediction.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.upc.quadrapp.prediction.PredictionTestSupport.occupy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With a model in the model directory, the scheduled run uses it (PRIMARY_MODEL, its version and
 * confidence 1 - MAE/25), and the stateless arrival advice reads the latest forecasts. The test
 * model predicts {@code 0.5 * occupancy_pct + horizon}; its MAE is 2.5, 5, 10 and 30.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"quadrapp.scheduling.enabled=false", "quadrapp.events.retry.min-age=PT0S"})
class ModelForecastAndArrivalAdviceIntegrationTest {

    private static final Path MODEL_DIRECTORY = modelDirectory();

    @DynamicPropertySource
    static void model(DynamicPropertyRegistry registry) {
        registry.add("quadrapp.prediction.model.directory", MODEL_DIRECTORY::toString);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OccupancyCommandService occupancy;

    @Autowired
    private ForecastCommandService forecasts;

    @MockitoBean
    private CurrentUserService currentUser;

    private OccupancyTestLayout own;
    private OccupancyTestLayout other;

    @BeforeEach
    void lots() {
        own = OccupancyTestLayout.create(jdbc, 4, 10);
        other = OccupancyTestLayout.create(jdbc, 4, 10);
        when(currentUser.requireAuthenticatedUserId()).thenReturn(own.userId);
    }

    @Test
    void theScheduledRunUsesTheLoadedModelAndTheResponseNamesItsVersion() throws Exception {
        occupy(occupancy, own, 0, 1, 2);

        forecasts.generateScheduledRuns();

        // 75 % occupied now: 0.5 * 75 + h.
        mvc.perform(get("/api/v1/parking-lots/{id}/forecasts", own.parkingLotId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].horizonMinutes").value(15))
                .andExpect(jsonPath("$[0].expectedOccupancyPct").value(52.5))
                .andExpect(jsonPath("$[0].method").value("PRIMARY_MODEL"))
                .andExpect(jsonPath("$[0].modelVersion").value("test-v1"))
                .andExpect(jsonPath("$[0].confidence").value(0.9))
                .andExpect(jsonPath("$[0].confidenceLevel").value("HIGH"))
                .andExpect(jsonPath("$[2].confidence").value(0.6))
                .andExpect(jsonPath("$[2].confidenceLevel").value("MEDIUM"))
                .andExpect(jsonPath("$[3].expectedOccupancyPct").value(97.5))
                .andExpect(jsonPath("$[3].confidence").value(0.0))
                .andExpect(jsonPath("$[3].confidenceLevel").value("LOW"))
                .andExpect(jsonPath("$[3].saturationExpectedAt").isNotEmpty())
                .andExpect(jsonPath("$[3].trigger").value("SCHEDULED"));
    }

    @Test
    void theArrivalAdviceUsesTheSmallestHorizonThatCoversTheEta() throws Exception {
        occupy(occupancy, own, 0, 1, 2);
        forecasts.generateScheduledRuns();

        // ETA 20 -> horizon 30: 0.5 * 75 + 30 = 67.5 % <= 90 % threshold, so space is likely.
        mvc.perform(advice("{\"lotId\": %d, \"etaMinutes\": 20}".formatted(own.parkingLotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etaMinutes").value(20))
                .andExpect(jsonPath("$.horizonUsed").value(30))
                .andExpect(jsonPath("$.probabilityOfSpace").value(1.0))
                .andExpect(jsonPath("$.category").value("HIGH"))
                .andExpect(jsonPath("$.confidence.value").value(0.8))
                .andExpect(jsonPath("$.confidence.level").value("HIGH"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.serverTime").isNotEmpty());
        mvc.perform(advice("{\"lotId\": %d, \"etaMinutes\": 0}".formatted(own.parkingLotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizonUsed").value(15));
        mvc.perform(advice("{\"lotId\": %d, \"etaMinutes\": 120}".formatted(own.parkingLotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizonUsed").value(60));
    }

    @Test
    void anInvalidAdviceRequestIs400AndAnotherInstitutionsLotOrNoForecastIs404() throws Exception {
        for (String body : new String[]{
                "{\"lotId\": %d, \"etaMinutes\": 121}",
                "{\"lotId\": %d, \"etaMinutes\": -1}",
                "{\"lotId\": %d, \"etaMinutes\": 20.5}",
                "{\"lotId\": %d}",
                "{\"lotId\": %d, \"etaMinutes\": 20, \"latitude\": -12.1}"}) {
            mvc.perform(advice(body.formatted(own.parkingLotId))).andExpect(status().isBadRequest());
        }
        mvc.perform(advice("{\"lotId\": %d, \"etaMinutes\": 20}".formatted(other.parkingLotId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Parking lot not found."));
        mvc.perform(advice("{\"lotId\": %d, \"etaMinutes\": 20}".formatted(own.parkingLotId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Arrival advice not available."));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder advice(String body) {
        return post("/api/v1/arrival-advices").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static Path modelDirectory() {
        try {
            Path directory = Files.createTempDirectory("quadrapp-model-");
            for (String name : new String[]{"model-v1.onnx", "model-v1.json"}) {
                try (InputStream in = Objects.requireNonNull(
                        ModelForecastAndArrivalAdviceIntegrationTest.class.getResourceAsStream("/prediction/model/" + name))) {
                    Files.copy(in, directory.resolve(name));
                }
            }
            directory.toFile().deleteOnExit();
            directory.resolve("model-v1.onnx").toFile().deleteOnExit();
            directory.resolve("model-v1.json").toFile().deleteOnExit();
            return directory;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

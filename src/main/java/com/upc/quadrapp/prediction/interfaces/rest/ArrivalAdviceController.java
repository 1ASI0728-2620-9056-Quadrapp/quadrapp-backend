package com.upc.quadrapp.prediction.interfaces.rest;

import com.upc.quadrapp.prediction.domain.services.ForecastQueryService;
import com.upc.quadrapp.prediction.interfaces.rest.resources.ArrivalAdviceResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Stateless arrival advice (US13-US15). The body has exactly {@code lotId} and
 * {@code etaMinutes} (an integer from 0 to 120); any other field, such as coordinates, is
 * rejected, and nothing of the request is stored.
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@Tag(name = "Prediction & Advisory", description = "Parking occupancy forecasts")
public class ArrivalAdviceController {

    private static final Set<String> FIELDS = Set.of("lotId", "etaMinutes");

    private final ForecastQueryService queries;

    public ArrivalAdviceController(ForecastQueryService queries) {
        this.queries = queries;
    }

    @Operation(summary = "Probability of finding space when arriving in etaMinutes; not stored")
    @PostMapping(
            value = "/api/v1/arrival-advices",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArrivalAdviceResource> requestAdvice(@RequestBody Map<String, Object> body) {
        if (!FIELDS.equals(body.keySet())) {
            throw new IllegalArgumentException("The request must have exactly lotId and etaMinutes.");
        }
        Long lotId = wholeNumber(body.get("lotId"), "lotId");
        Long eta = wholeNumber(body.get("etaMinutes"), "etaMinutes");
        if (lotId <= 0) {
            throw new IllegalArgumentException("lotId must be positive.");
        }
        if (eta < 0 || eta > 120) {
            throw new IllegalArgumentException("etaMinutes must be between 0 and 120.");
        }
        return ResponseEntity.ok(ArrivalAdviceResource.from(
                queries.adviseArrival(lotId, eta.intValue()),
                Instant.now()
        ));
    }

    private static Long wholeNumber(Object value, String name) {
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException(name + " must be an integer.");
    }
}

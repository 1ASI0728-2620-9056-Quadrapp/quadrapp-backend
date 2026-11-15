package com.upc.quadrapp.prediction.interfaces.rest;

import com.upc.quadrapp.prediction.domain.model.commands.GenerateForecastsCommand;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import com.upc.quadrapp.prediction.domain.services.ForecastQueryService;
import com.upc.quadrapp.prediction.interfaces.rest.resources.ForecastAccuracyMetricsResource;
import com.upc.quadrapp.prediction.interfaces.rest.resources.ForecastResource;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Prediction & Advisory", description = "Parking occupancy forecasts")
public class ForecastController {

    private final ForecastCommandService commands;
    private final ForecastQueryService queries;

    @PostMapping("/api/v1/parking-lots/{parkingLotId}/forecasts")
    public ResponseEntity<List<ForecastResource>> generate(
            @PathVariable Long parkingLotId
    ) {
        List<ForecastResource> forecasts = commands
                .handle(new GenerateForecastsCommand(parkingLotId))
                .stream()
                .map(ForecastResource::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(forecasts);
    }

    @GetMapping("/api/v1/parking-lots/{parkingLotId}/forecasts")
    public ResponseEntity<List<ForecastResource>> latest(
            @PathVariable Long parkingLotId
    ) {
        return ResponseEntity.ok(
                queries.findLatestByParkingLot(parkingLotId)
                        .stream()
                        .map(ForecastResource::from)
                        .toList()
        );
    }

    @GetMapping("/api/v1/parking-lots/{parkingLotId}/forecasts/metrics")
    public ResponseEntity<ForecastAccuracyMetricsResource> accuracyMetrics(
            @PathVariable Long parkingLotId
    ) {
        return ResponseEntity.ok(
                ForecastAccuracyMetricsResource.from(
                        queries.accuracyMetricsByParkingLot(parkingLotId)
                )
        );
    }
}

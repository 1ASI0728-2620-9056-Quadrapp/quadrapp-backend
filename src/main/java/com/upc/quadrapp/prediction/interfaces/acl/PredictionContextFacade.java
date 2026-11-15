package com.upc.quadrapp.prediction.interfaces.acl;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;
import com.upc.quadrapp.prediction.domain.services.ForecastQueryService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class PredictionContextFacade {

    private final ForecastQueryService queries;

    public PredictionContextFacade(ForecastQueryService queries) {
        this.queries = queries;
    }

    public List<ForecastView> latestForecasts(Long parkingLotId) {
        return queries.findLatestByParkingLot(parkingLotId)
                .stream()
                .map(ForecastView::from)
                .toList();
    }

    public AccuracyMetricsView accuracyMetrics(Long parkingLotId) {
        return AccuracyMetricsView.from(queries.accuracyMetricsByParkingLot(parkingLotId));
    }

    public record ForecastView(
            Long id,
            Long parkingLotId,
            int horizonMinutes,
            Instant generatedAt,
            Instant targetAt,
            BigDecimal expectedOccupancyPct,
            BigDecimal confidence,
            String confidenceLevel,
            String method,
            String modelVersion,
            Instant saturationExpectedAt,
            BigDecimal observedOccupancyPct,
            BigDecimal absoluteError
    ) {
        private static ForecastView from(Forecast forecast) {
            return new ForecastView(
                    forecast.id(),
                    forecast.parkingLotId(),
                    forecast.horizonMinutes(),
                    forecast.generatedAt(),
                    forecast.targetAt(),
                    forecast.expectedOccupancyPct(),
                    forecast.confidence(),
                    forecast.confidenceLevel().name(),
                    forecast.method().name(),
                    forecast.modelVersion(),
                    forecast.saturationExpectedAt(),
                    forecast.observedOccupancyPct(),
                    forecast.absoluteError()
            );
        }
    }

    public record AccuracyMetricsView(
            long evaluatedForecasts,
            BigDecimal meanAbsoluteErrorPct
    ) {
        private static AccuracyMetricsView from(ForecastAccuracyMetrics metrics) {
            return new AccuracyMetricsView(
                    metrics.evaluatedForecasts(),
                    metrics.meanAbsoluteErrorPct()
            );
        }
    }
}

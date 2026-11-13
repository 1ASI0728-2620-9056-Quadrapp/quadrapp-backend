package com.upc.quadrapp.prediction.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.prediction.application.internal.outboundservices.persistence.ForecastPersistencePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastAccuracyMetrics;
import com.upc.quadrapp.prediction.infrastructure.persistence.jpa.entities.ForecastJpaEntity;
import com.upc.quadrapp.prediction.infrastructure.persistence.jpa.repositories.ForecastJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ForecastPersistenceAdapter implements ForecastPersistencePort {

    private final ForecastJpaRepository repository;

    public ForecastPersistenceAdapter(ForecastJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Forecast save(Forecast forecast) {
        ForecastJpaEntity entity = toJpa(forecast);
        return toDomain(repository.save(entity));
    }

    @Override
    public List<Forecast> findLatestByParkingLot(Long parkingLotId) {
        return repository.findLatestByParkingLot(parkingLotId)
                .stream()
                .map(ForecastPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Forecast> findDueUnobserved(Instant now, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Forecast batch size must be positive.");
        }
        return repository.findDueUnobserved(now, PageRequest.of(0, limit))
                .stream()
                .map(ForecastPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public ForecastAccuracyMetrics accuracyMetricsByParkingLot(Long parkingLotId) {
        ForecastJpaRepository.AccuracyMetricsProjection metrics =
                repository.accuracyMetricsByParkingLot(parkingLotId);
        return new ForecastAccuracyMetrics(
                metrics.getEvaluatedForecasts(),
                metrics.getMeanAbsoluteErrorPct()
        );
    }

    private static ForecastJpaEntity toJpa(Forecast forecast) {
        ForecastJpaEntity entity = new ForecastJpaEntity();
        entity.setId(forecast.id());
        entity.setParkingLotId(forecast.parkingLotId());
        entity.setHorizonMinutes((short) forecast.horizonMinutes());
        entity.setGeneratedAt(forecast.generatedAt());
        entity.setTargetAt(forecast.targetAt());
        entity.setExpectedOccupancyPct(forecast.expectedOccupancyPct());
        entity.setConfidence(forecast.confidence());
        entity.setConfidenceLevel(forecast.confidenceLevel());
        entity.setMethod(forecast.method());
        entity.setModelVersion(forecast.modelVersion());
        entity.setSaturationExpectedAt(forecast.saturationExpectedAt());
        entity.setObservedOccupancyPct(forecast.observedOccupancyPct());
        entity.setAbsoluteError(forecast.absoluteError());
        return entity;
    }

    private static Forecast toDomain(ForecastJpaEntity entity) {
        return new Forecast(
                entity.getId(),
                entity.getParkingLotId(),
                entity.getHorizonMinutes(),
                entity.getGeneratedAt(),
                entity.getTargetAt(),
                entity.getExpectedOccupancyPct(),
                entity.getConfidence(),
                entity.getConfidenceLevel(),
                entity.getMethod(),
                entity.getModelVersion(),
                entity.getSaturationExpectedAt(),
                entity.getObservedOccupancyPct(),
                entity.getAbsoluteError()
        );
    }
}

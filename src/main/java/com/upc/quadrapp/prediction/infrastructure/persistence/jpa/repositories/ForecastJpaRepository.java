package com.upc.quadrapp.prediction.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.prediction.infrastructure.persistence.jpa.entities.ForecastJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ForecastJpaRepository extends JpaRepository<ForecastJpaEntity, Long> {

    interface AccuracyMetricsProjection {
        Long getEvaluatedForecasts();

        BigDecimal getMeanAbsoluteErrorPct();
    }

    @Query(value = """
            SELECT DISTINCT ON (horizon_min) *
            FROM prediction.forecasts
            WHERE lot_id = :parkingLotId
            ORDER BY horizon_min, generated_at DESC
            """, nativeQuery = true)
    List<ForecastJpaEntity> findLatestByParkingLot(
            @Param("parkingLotId") Long parkingLotId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT forecast
            FROM ForecastJpaEntity forecast
            WHERE forecast.targetAt <= :now
              AND forecast.observedOccupancyPct IS NULL
            ORDER BY forecast.targetAt
            """)
    List<ForecastJpaEntity> findDueUnobserved(
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(abs_error) AS "evaluatedForecasts",
                   AVG(abs_error) AS "meanAbsoluteErrorPct"
            FROM prediction.forecasts
            WHERE lot_id = :parkingLotId
              AND abs_error IS NOT NULL
            """, nativeQuery = true)
    AccuracyMetricsProjection accuracyMetricsByParkingLot(
            @Param("parkingLotId") Long parkingLotId
    );
}

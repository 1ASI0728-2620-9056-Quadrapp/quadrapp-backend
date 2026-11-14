package com.upc.quadrapp.prediction.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastTrigger;
import com.upc.quadrapp.prediction.infrastructure.persistence.jpa.entities.ForecastJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<ForecastJpaEntity> findByParkingLotIdAndRunSlotAndTriggerOrderByHorizonMinutes(
            Long parkingLotId,
            Instant runSlot,
            ForecastTrigger trigger
    );

    /** Uses the partial index on {@code target_at} of the forecasts not evaluated yet. */
    @Query("""
            SELECT forecast
            FROM ForecastJpaEntity forecast
            WHERE forecast.targetAt <= :cutoff
              AND forecast.evaluatedAt IS NULL
            ORDER BY forecast.targetAt
            """)
    List<ForecastJpaEntity> findDueUnevaluated(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE ForecastJpaEntity forecast
            SET forecast.evaluatedAt = :evaluatedAt
            WHERE forecast.id = :id AND forecast.evaluatedAt IS NULL
            """)
    int markEvaluated(@Param("id") Long id, @Param("evaluatedAt") Instant evaluatedAt);

    @Modifying
    @Query(value = "DELETE FROM prediction.forecasts WHERE generated_at < :cutoff", nativeQuery = true)
    int deleteGeneratedBefore(@Param("cutoff") Instant cutoff);

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

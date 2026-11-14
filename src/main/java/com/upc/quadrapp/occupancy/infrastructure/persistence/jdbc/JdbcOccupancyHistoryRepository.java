package com.upc.quadrapp.occupancy.infrastructure.persistence.jdbc;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.OccupancyHistoryPersistencePort;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyCounts;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Reads of the snapshot history that use the {@code (parking_lot_id, taken_at)} index: each one
 * scans only the minutes of the requested windows.
 */
@Repository
public class JdbcOccupancyHistoryRepository implements OccupancyHistoryPersistencePort {

    private static final String CLOSEST_SNAPSHOT = """
            SELECT s.taken_at,
                   SUM(s.total_spaces) AS total_spaces,
                   SUM(s.free_count) AS free_count,
                   SUM(s.occupied_count) AS occupied_count,
                   SUM(s.unknown_count) AS unknown_count
            FROM occupancy.occupancy_snapshots s
            WHERE s.parking_lot_id = :lot
              AND s.taken_at = (
                  SELECT n.taken_at
                  FROM occupancy.occupancy_snapshots n
                  WHERE n.parking_lot_id = :lot
                    AND n.taken_at BETWEEN :from AND :to
                  ORDER BY abs(extract(epoch FROM (n.taken_at - :at))), n.taken_at
                  LIMIT 1)
            GROUP BY s.taken_at
            """;

    private static final String WEEKLY_SLOT_AVERAGE = """
            SELECT COUNT(DISTINCT m.week) AS weeks, AVG(m.pct) AS average
            FROM (
                SELECT w.k AS week,
                       s.taken_at,
                       SUM(s.occupied_count) * 100.0
                           / NULLIF(SUM(s.free_count + s.occupied_count), 0) AS pct
                FROM generate_series(1, :weeks) AS w(k)
                JOIN occupancy.occupancy_snapshots s
                  ON s.parking_lot_id = :lot
                 AND s.taken_at >= :slotStart - make_interval(weeks => w.k)
                 AND s.taken_at < :slotStart - make_interval(weeks => w.k)
                                  + make_interval(secs => CAST(:slotSeconds AS double precision))
                GROUP BY w.k, s.taken_at
            ) m
            WHERE m.pct IS NOT NULL
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcOccupancyHistoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LotSnapshot> findLotSnapshotClosestTo(Long parkingLotId, Instant at, Duration tolerance) {
        var params = new MapSqlParameterSource()
                .addValue("lot", parkingLotId)
                .addValue("at", utc(at))
                .addValue("from", utc(at.minus(tolerance)))
                .addValue("to", utc(at.plus(tolerance)));
        return jdbc.query(CLOSEST_SNAPSHOT, params, (rs, row) -> new LotSnapshot(
                        rs.getObject("taken_at", OffsetDateTime.class).toInstant(),
                        new OccupancyCounts(
                                rs.getLong("total_spaces"),
                                rs.getLong("free_count"),
                                rs.getLong("occupied_count"),
                                rs.getLong("unknown_count")
                        )))
                .stream()
                .findFirst();
    }

    @Override
    public WeeklySlotAverage weeklySlotAverage(
            Long parkingLotId,
            Instant slotStart,
            Duration slotLength,
            int weeks
    ) {
        var params = new MapSqlParameterSource()
                .addValue("lot", parkingLotId)
                .addValue("slotStart", utc(slotStart))
                .addValue("slotSeconds", slotLength.toSeconds())
                .addValue("weeks", weeks);
        return jdbc.queryForObject(WEEKLY_SLOT_AVERAGE, params, (rs, row) -> {
            BigDecimal average = rs.getBigDecimal("average");
            return new WeeklySlotAverage(
                    rs.getInt("weeks"),
                    average == null ? null : average.setScale(2, RoundingMode.HALF_UP)
            );
        });
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}

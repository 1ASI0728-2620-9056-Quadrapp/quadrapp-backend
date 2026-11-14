package com.upc.quadrapp.prediction;

import com.upc.quadrapp.occupancy.OccupancyTestLayout;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;
import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import com.upc.quadrapp.occupancy.domain.services.OccupancyCommandService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Arranges occupancy state and snapshot history for the Prediction integration tests. */
public final class PredictionTestSupport {

    private PredictionTestSupport() {
    }

    /** Marks the spaces with the given indexes occupied and every other active space free. */
    public static void occupy(OccupancyCommandService occupancy, OccupancyTestLayout lot, int... occupied) {
        Instant at = Instant.now().minusSeconds(60);
        for (int i = 0; i < lot.spaceIds.size(); i++) {
            final int index = i;
            boolean isOccupied = java.util.Arrays.stream(occupied).anyMatch(o -> o == index);
            occupancy.handle(new RecordSensorEventCommand(lot.institutionId, lot.parkingLotId, 1L, lot.space(i), null,
                    UUID.randomUUID().toString(),
                    isOccupied ? OccupancyEventType.OCCUPIED : OccupancyEventType.FREED, at));
        }
    }

    /**
     * Saves one snapshot per minute with the given occupancy for {@code minutes} minutes from
     * {@code weeksBack} weeks before now, so every 15-minute slot of the next hour has history.
     */
    public static void history(JdbcTemplate jdbc, OccupancyTestLayout lot, int weeksBack, int free, int occupied,
                               int minutes) {
        Instant start = Instant.now().minus(7L * weeksBack, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES)
                .minus(1, ChronoUnit.MINUTES);
        for (int minute = 0; minute <= minutes; minute++) {
            jdbc.update("insert into occupancy.occupancy_snapshots (parking_lot_id, zone_id, taken_at, total_spaces, "
                            + "free_count, occupied_count, unknown_count) values (?, ?, ?, ?, ?, ?, 0)",
                    lot.parkingLotId, lot.zoneId, start.plus(minute, ChronoUnit.MINUTES).atOffset(ZoneOffset.UTC),
                    free + occupied, free, occupied);
        }
    }
}

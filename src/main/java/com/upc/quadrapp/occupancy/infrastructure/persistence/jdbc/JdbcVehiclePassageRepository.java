package com.upc.quadrapp.occupancy.infrastructure.persistence.jdbc;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.persistence.VehiclePassagePersistencePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

/** Vehicle passages, kept 7 days; the eventId of Parking Sensing keeps each passage unique. */
@Repository
public class JdbcVehiclePassageRepository implements VehiclePassagePersistencePort {

    private final JdbcTemplate jdbc;

    public JdbcVehiclePassageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean appendIfAbsent(String eventId, Long tenantId, Long parkingLotId, Long accessPointId,
                                  boolean entering, Instant passedAt, Instant recordedAt) {
        return jdbc.update("""
                INSERT INTO occupancy.vehicle_passages
                    (event_id, tenant_id, parking_lot_id, access_point_id, direction, passed_at, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, tenantId, parkingLotId, accessPointId, entering ? "IN" : "OUT",
                Timestamp.from(passedAt), Timestamp.from(recordedAt)) == 1;
    }

    @Override
    public int deletePassedBefore(Instant cutoff) {
        return jdbc.update("DELETE FROM occupancy.vehicle_passages WHERE passed_at < ?", Timestamp.from(cutoff));
    }
}

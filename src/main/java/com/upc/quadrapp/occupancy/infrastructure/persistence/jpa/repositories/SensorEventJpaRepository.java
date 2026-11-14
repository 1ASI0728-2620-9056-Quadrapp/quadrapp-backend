package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities.SensorEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SensorEventJpaRepository extends JpaRepository<SensorEventJpaEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO occupancy.sensor_events
                (event_id, device_id, space_id, access_id, event_type, occurred_at, received_at)
            VALUES
                (:eventId, :deviceId, :spaceId, :accessId, :eventType, :occurredAt, :receivedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("deviceId") Long deviceId,
            @Param("spaceId") Long spaceId,
            @Param("accessId") Long accessId,
            @Param("eventType") String eventType,
            @Param("occurredAt") java.time.Instant occurredAt,
            @Param("receivedAt") java.time.Instant receivedAt
    );
}

package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.occupancy.domain.model.valueobjects.OccupancyEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "sensor_events",
        uniqueConstraints = @UniqueConstraint(
                name = "sensor_events_event_id_key",
                columnNames = "event_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class SensorEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "space_id")
    private Long spaceId;

    @Column(name = "access_id")
    private Long accessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 10)
    private OccupancyEventType eventType;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamptz")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, columnDefinition = "timestamptz")
    private Instant receivedAt;
}

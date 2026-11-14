package com.upc.quadrapp.occupancy.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        schema = "occupancy",
        name = "occupancy_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_occupancy_snapshots_zone_taken",
                columnNames = {"zone_id", "taken_at"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class OccupancySnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "taken_at", nullable = false, columnDefinition = "timestamptz")
    private Instant takenAt;

    @Column(name = "total_spaces", nullable = false)
    private short totalSpaces;

    @Column(name = "free_count", nullable = false)
    private short freeCount;

    @Column(name = "occupied_count", nullable = false)
    private short occupiedCount;

    @Column(name = "unknown_count", nullable = false)
    private short unknownCount;
}

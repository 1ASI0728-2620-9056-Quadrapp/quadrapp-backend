package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "zones", uniqueConstraints = @UniqueConstraint(name = "uk_zones_lot_name", columnNames = {"lot_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class ZoneJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "description", length = 80)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;
}

package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.AccessType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "accesses", uniqueConstraints = @UniqueConstraint(name = "uk_accesses_lot_name", columnNames = {"lot_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class AccessJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long parkingLotId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 15)
    private AccessType type;

    @Column(name = "flow_counting_enabled", nullable = false)
    private boolean flowCountingEnabled;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;
}


package com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(schema = "iam", name = "institutions", uniqueConstraints = @UniqueConstraint(name = "uk_institutions_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class InstitutionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "active", nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz default now()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz default now()")

    private Instant updatedAt;
}

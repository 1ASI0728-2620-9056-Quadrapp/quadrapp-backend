
package com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "iam", name = "institution_domains", uniqueConstraints = @UniqueConstraint(name = "uk_institution_domains_domain", columnNames = "domain"),
        check = @CheckConstraint(name = "ck_institution_domains_lowercase", constraint = "domain = lower(domain)"))
@Getter
@Setter
@NoArgsConstructor
public class InstitutionDomainJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_institution_domains_institution"))
    private InstitutionJpaEntity institution;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_institution_domains_verifier"))
    private UserJpaEntity verifier;

    @Column(name = "domain", nullable = false, length = 120)
    private String domain;

    @Column(name = "verified_at", columnDefinition = "timestamptz")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz default now()")
    private Instant createdAt;
}


package com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities;

import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;
import com.upc.quadrapp.iam.domain.model.valueobjects.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(
        schema = "iam",
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uk_users_invitation_token_hash",
                        columnNames = "invitation_token_hash"
                )
        },
        check = {
                @CheckConstraint(
                        name = "ck_users_lowercase_email",
                        constraint = "email = lower(email)"
                ),
                @CheckConstraint(
                        name = "ck_users_status",
                        constraint = "status in ('INVITED', 'ACTIVE', 'DISABLED')"
                ),
                @CheckConstraint(
                        name = "ck_users_role",
                        constraint = "role in ('DRIVER', 'OPERATOR', 'ADMIN', 'PLATFORM_ADMIN')"
                ),
                @CheckConstraint(
                        name = "ck_users_locale",
                        constraint = "locale in ('es-419', 'en-US')"
                ),
                @CheckConstraint(
                        name = "ck_users_invitation",
                        constraint = "status <> 'INVITED' OR " +
                                "(invitation_token_hash IS NOT NULL AND " +
                                "invitation_expires_at IS NOT NULL)"
                ),
                @CheckConstraint(
                        name = "ck_users_platform_admin",
                        constraint = "((role = 'PLATFORM_ADMIN') = " +
                                "(institution_id IS NULL))"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id")
    private Long institutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "institution_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_users_institution"
            )
    )
    private InstitutionJpaEntity institution;

    @Column(name = "invited_by")
    private Long invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "invited_by",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_users_inviter"
            )
    )
    private UserJpaEntity inviter;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 15)
    private Roles role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @ColumnDefault("'ACTIVE'")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "invitation_token_hash", length = 255)
    private String invitationTokenHash;

    @Column(
            name = "invitation_expires_at",
            columnDefinition = "timestamptz"
    )
    private Instant invitationExpiresAt;

    @Column(
            name = "verified_at",
            columnDefinition = "timestamptz"
    )
    private Instant verifiedAt;

    @Column(
            name = "last_login_at",
            columnDefinition = "timestamptz"
    )
    private Instant lastLoginAt;

    @Column(name = "locale", nullable = false, length = 6)
    @ColumnDefault("'es-419'")
    private String locale = "es-419";


    @Column(name = "push_enabled", nullable = false)
    @ColumnDefault("true")
    private boolean pushEnabled = true;

    @Column(name = "saturation_alerts", nullable = false)
    @ColumnDefault("true")
    private boolean saturationAlerts = true;

    @Column(name = "low_arrival_alerts", nullable = false)
    @ColumnDefault("true")
    private boolean lowArrivalAlerts = true;

    @Column(name = "stale_data_alerts", nullable = false)
    @ColumnDefault("true")
    private boolean staleDataAlerts = true;

    @Column(name = "event_alerts", nullable = false)
    @ColumnDefault("false")
    private boolean eventAlerts = false;


    @Column(
            name = "created_at",
            nullable = false,
            columnDefinition = "timestamptz default now()"
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "timestamptz default now()"
    )
    private Instant updatedAt;
}

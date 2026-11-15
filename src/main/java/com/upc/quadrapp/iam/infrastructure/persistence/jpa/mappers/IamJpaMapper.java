package com.upc.quadrapp.iam.infrastructure.persistence.jpa.mappers;

import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.domain.model.entities.RefreshToken;
import com.upc.quadrapp.iam.domain.model.valueobjects.NotificationPreferences;

import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.InstitutionJpaEntity;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.InstitutionDomainJpaEntity;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.UserJpaEntity;
import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.RefreshTokenJpaEntity;

import org.springframework.stereotype.Component;

@Component
public class IamJpaMapper {

    public Institution toDomain(InstitutionJpaEntity jpa) {
        return Institution.restore(
                jpa.getId(),
                jpa.getName(),
                jpa.isActive(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public InstitutionJpaEntity toJpa(Institution domain) {
        var jpa = new InstitutionJpaEntity();

        jpa.setId(domain.getId());
        jpa.setName(domain.getName());
        jpa.setActive(domain.isActive());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());

        return jpa;
    }

    public InstitutionDomain toDomain(
            InstitutionDomainJpaEntity jpa
    ) {
        return InstitutionDomain.restore(
                jpa.getId(),
                jpa.getInstitutionId(),
                jpa.getDomain(),
                jpa.getVerifiedBy(),
                jpa.getVerifiedAt(),
                jpa.getCreatedAt()
        );
    }

    public InstitutionDomainJpaEntity toJpa(
            InstitutionDomain domain
    ) {
        var jpa = new InstitutionDomainJpaEntity();

        jpa.setId(domain.getId());
        jpa.setInstitutionId(domain.getInstitutionId());
        jpa.setVerifiedBy(domain.getVerifiedBy());
        jpa.setDomain(domain.getDomain().value());
        jpa.setVerifiedAt(domain.getVerifiedAt());
        jpa.setCreatedAt(domain.getCreatedAt());

        return jpa;
    }

    public User toDomain(UserJpaEntity jpa) {
        return User.restore(
                jpa.getId(),
                jpa.getInstitutionId(),
                jpa.getInvitedBy(),
                jpa.getEmail(),
                jpa.getFullName(),
                jpa.getRole(),
                jpa.getStatus(),
                jpa.getInvitationTokenHash(),
                jpa.getInvitationExpiresAt(),
                jpa.getVerifiedAt(),
                jpa.getLastLoginAt(),
                jpa.getLocale(),
                new NotificationPreferences(
                        jpa.isPushEnabled(),
                        jpa.isSaturationAlerts(),
                        jpa.isLowArrivalAlerts(),
                        jpa.isStaleDataAlerts(),
                        jpa.isEventAlerts()
                ),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public UserJpaEntity toJpa(User domain) {
        var jpa = new UserJpaEntity();

        jpa.setId(domain.getId());
        jpa.setInstitutionId(domain.getInstitutionId());
        jpa.setInvitedBy(domain.getInvitedBy());
        jpa.setEmail(domain.getEmail().value());
        jpa.setFullName(domain.getFullName());
        jpa.setRole(domain.getRole());
        jpa.setStatus(domain.getStatus());

        jpa.setInvitationTokenHash(
                domain.getInvitationTokenHash()
        );
        jpa.setInvitationExpiresAt(
                domain.getInvitationExpiresAt()
        );

        jpa.setVerifiedAt(domain.getVerifiedAt());
        jpa.setLastLoginAt(domain.getLastLoginAt());
        jpa.setLocale(domain.getLocale());

        var preferences = domain.getNotificationPreferences();

        jpa.setPushEnabled(preferences.pushEnabled());
        jpa.setSaturationAlerts(preferences.saturationAlerts());
        jpa.setLowArrivalAlerts(preferences.lowArrivalAlerts());
        jpa.setStaleDataAlerts(preferences.staleDataAlerts());
        jpa.setEventAlerts(preferences.eventAlerts());

        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());

        return jpa;
    }

    public RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        return RefreshToken.restore(
                jpa.getId(),
                jpa.getUserId(),
                jpa.getFamilyId(),
                jpa.getTokenHash(),
                jpa.getExpiresAt(),
                jpa.getRevokedAt(),
                jpa.getCreatedAt()
        );
    }

    public RefreshTokenJpaEntity toJpa(RefreshToken domain) {
        var jpa = new RefreshTokenJpaEntity();

        jpa.setId(domain.getId());
        jpa.setUserId(domain.getUserId());
        jpa.setFamilyId(domain.getFamilyId());
        jpa.setTokenHash(domain.getTokenHash());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setRevokedAt(domain.getRevokedAt());
        jpa.setCreatedAt(domain.getCreatedAt());

        return jpa;
    }
}

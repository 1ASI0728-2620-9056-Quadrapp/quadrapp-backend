package com.upc.quadrapp.iam.domain.model.entities;

import com.upc.quadrapp.iam.domain.model.valueobjects.EmailAddress;
import com.upc.quadrapp.iam.domain.model.valueobjects.InstitutionDomainName;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public class InstitutionDomain {

    private final Long id;
    private final Long institutionId;
    private final InstitutionDomainName domain;

    private Long verifiedBy;
    private Instant verifiedAt;
    private final Instant createdAt;

    private InstitutionDomain(
            Long id,
            Long institutionId,
            InstitutionDomainName domain,
            Long verifiedBy,
            Instant verifiedAt,
            Instant createdAt
    ) {
        validateOptionalId(id);
        requirePositiveId(institutionId);
        validateOptionalId(verifiedBy);

        this.id = id;
        this.institutionId = institutionId;
        this.domain = Objects.requireNonNull(
                domain, "Institution domain is required."
        );

        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation date is required."
        );
    }

    public static InstitutionDomain create(
            Long institutionId,
            String domain,
            Instant now
    ) {
        return new InstitutionDomain(
                null,
                institutionId,
                new InstitutionDomainName(domain),
                null,
                null,
                now
        );
    }

    public static InstitutionDomain restore(
            Long id,
            Long institutionId,
            String domain,
            Long verifiedBy,
            Instant verifiedAt,
            Instant createdAt
    ) {
        requirePositiveId(id);

        return new InstitutionDomain(
                id,
                institutionId,
                new InstitutionDomainName(domain),
                verifiedBy,
                verifiedAt,
                createdAt
        );
    }

    public void verify(Long verifierId, Instant now) {
        requirePositiveId(verifierId);
        Objects.requireNonNull(now, "Timestamp is required.");

        if (isVerified()) {
            throw new IllegalStateException(
                    "Institution domain is already verified."
            );
        }

        this.verifiedBy = verifierId;
        this.verifiedAt = now;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean allows(EmailAddress email) {
        return isVerified() && domain.matches(email);
    }

    private static void validateOptionalId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    "Identifier must be positive."
            );
        }
    }

    private static void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Valid identifier is required."
            );
        }
    }
}


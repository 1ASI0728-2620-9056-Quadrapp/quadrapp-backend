package com.upc.quadrapp.iam.domain.model.aggregates;

import com.upc.quadrapp.iam.domain.model.exceptions.IamConflictException;
import com.upc.quadrapp.iam.domain.model.valueobjects.EmailAddress;
import com.upc.quadrapp.iam.domain.model.valueobjects.NotificationPreferences;
import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;
import com.upc.quadrapp.iam.domain.model.valueobjects.UserStatus;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Objects;

@Getter
public class User {

    private final Long id;
    private final Long institutionId;
    private final Long invitedBy;
    private final EmailAddress email;

    private String fullName;
    private Roles role;
    private UserStatus status;

    private String invitationTokenHash;
    private Instant invitationExpiresAt;

    private Instant verifiedAt;
    private Instant lastLoginAt;

    private String locale;
    private NotificationPreferences notificationPreferences;

    private final Instant createdAt;
    private Instant updatedAt;

    private User(
            Long id,
            Long institutionId,
            Long invitedBy,
            EmailAddress email,
            String fullName,
            Roles role,
            UserStatus status,
            String invitationTokenHash,
            Instant invitationExpiresAt,
            Instant verifiedAt,
            Instant lastLoginAt,
            String locale,
            NotificationPreferences notificationPreferences,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateOptionalId(id);
        validateOptionalId(invitedBy);

        this.id = id;
        this.institutionId = institutionId;
        this.invitedBy = invitedBy;
        this.email = Objects.requireNonNull(
                email, "Email is required."
        );

        this.fullName = validateOptionalFullName(fullName);
        this.role = Objects.requireNonNull(
                role, "Role is required."
        );
        this.status = Objects.requireNonNull(
                status, "User status is required."
        );

        this.invitationTokenHash = invitationTokenHash;
        this.invitationExpiresAt = invitationExpiresAt;

        this.verifiedAt = verifiedAt;
        this.lastLoginAt = lastLoginAt;

        this.locale = validateLocale(locale);
        this.notificationPreferences = Objects.requireNonNull(
                notificationPreferences,
                "Notification preferences are required."
        );

        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation date is required."
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt, "Update date is required."
        );

        validateInstitution();
        validateInvitation();
    }

    public static User registerDriver(
            Long institutionId,
            String email,
            Instant now
    ) {
        requirePositiveId(institutionId);
        Objects.requireNonNull(now, "Timestamp is required.");

        return new User(
                null,
                institutionId,
                null,
                new EmailAddress(email),
                null,
                Roles.DRIVER,
                UserStatus.ACTIVE,
                null,
                null,
                now,
                now,
                "es-419",
                NotificationPreferences.defaults(),
                now,
                now
        );
    }

    public static User createPlatformAdmin(
            String email,
            Instant now
    ) {
        Objects.requireNonNull(now, "Timestamp is required.");

        return new User(
                null,
                null,
                null,
                new EmailAddress(email),
                null,
                Roles.PLATFORM_ADMIN,
                UserStatus.ACTIVE,
                null,
                null,
                now,
                null,
                "es-419",
                NotificationPreferences.defaults(),
                now,
                now
        );
    }

    public static User invite(
            Long institutionId,
            Long invitedBy,
            String email,
            Roles role,
            String tokenHash,
            Instant expiresAt,
            Instant now
    ) {
        requirePositiveId(institutionId);
        requirePositiveId(invitedBy);
        Objects.requireNonNull(now, "Timestamp is required.");

        if (role == null || !role.canBeInvited()) {
            throw new IllegalArgumentException(
                    "Only administrators and operators can be invited."
            );
        }

        validateRequiredTokenHash(tokenHash);

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Invitation expiration must be in the future."
            );
        }

        return new User(
                null,
                institutionId,
                invitedBy,
                new EmailAddress(email),
                null,
                role,
                UserStatus.INVITED,
                tokenHash,
                expiresAt,
                null,
                null,
                "es-419",
                NotificationPreferences.defaults(),
                now,
                now
        );
    }

    public static User restore(
            Long id,
            Long institutionId,
            Long invitedBy,
            String email,
            String fullName,
            Roles role,
            UserStatus status,
            String invitationTokenHash,
            Instant invitationExpiresAt,
            Instant verifiedAt,
            Instant lastLoginAt,
            String locale,
            NotificationPreferences notificationPreferences,
            Instant createdAt,
            Instant updatedAt
    ) {
        requirePositiveId(id);

        return new User(
                id,
                institutionId,
                invitedBy,
                new EmailAddress(email),
                fullName,
                role,
                status,
                invitationTokenHash,
                invitationExpiresAt,
                verifiedAt,
                lastLoginAt,
                locale,
                notificationPreferences,
                createdAt,
                updatedAt
        );
    }

    /**
     * Sends the invitation again with a new token and expiry, for an invitation that expired or
     * never arrived. Only a pending invitation of the same institution can be renewed.
     */
    public void reissueInvitation(
            Long institutionId,
            Roles role,
            String tokenHash,
            Instant expiresAt,
            Instant now
    ) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (status != UserStatus.INVITED ||
                !Objects.equals(this.institutionId, institutionId)) {
            throw new IamConflictException(
                    "Email is already registered."
            );
        }

        if (role == null || !role.canBeInvited()) {
            throw new IllegalArgumentException(
                    "Only administrators and operators can be invited."
            );
        }

        validateRequiredTokenHash(tokenHash);

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Invitation expiration must be in the future."
            );
        }

        this.role = role;
        this.invitationTokenHash = tokenHash;
        this.invitationExpiresAt = expiresAt;
        this.updatedAt = now;
    }

    public void acceptInvitation(
            String providedTokenHash,
            String fullName,
            Instant now
    ) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (status != UserStatus.INVITED) {
            throw new IamConflictException(
                    "User has no pending invitation."
            );
        }

        if (invitationExpiresAt == null ||
                !now.isBefore(invitationExpiresAt)) {
            throw new IamConflictException(
                    "Invitation has expired."
            );
        }

        if (!matchesInvitationHash(providedTokenHash)) {
            throw new IllegalArgumentException(
                    "Invalid invitation token."
            );
        }

        this.fullName = validateRequiredFullName(fullName);
        this.status = UserStatus.ACTIVE;
        this.verifiedAt = now;

        this.invitationTokenHash = null;
        this.invitationExpiresAt = null;
        this.updatedAt = now;
    }

    public boolean matchesInvitationHash(String providedTokenHash) {
        if (providedTokenHash == null ||
                invitationTokenHash == null) {
            return false;
        }

        return MessageDigest.isEqual(
                invitationTokenHash.getBytes(StandardCharsets.UTF_8),
                providedTokenHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    public void registerSuccessfulLogin(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (status != UserStatus.ACTIVE) {
            throw new IamConflictException(
                    "User account is not active."
            );
        }

        if (verifiedAt == null) {
            verifiedAt = now;
        }

        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    public void updateFullName(String newFullName, Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        this.fullName = validateRequiredFullName(newFullName);
        this.updatedAt = now;
    }

    public void changeRole(Roles newRole, Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (role == Roles.PLATFORM_ADMIN ||
                newRole == null ||
                newRole == Roles.PLATFORM_ADMIN) {
            throw new IllegalArgumentException(
                    "Platform administrator role cannot be assigned here."
            );
        }

        if (status != UserStatus.ACTIVE) {
            throw new IamConflictException(
                    "Only active users can change roles."
            );
        }

        this.role = newRole;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (status == UserStatus.DISABLED) {
            throw new IamConflictException(
                    "User is already disabled."
            );
        }

        this.status = UserStatus.DISABLED;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        if (status != UserStatus.DISABLED) {
            throw new IamConflictException(
                    "Only disabled users can be reactivated."
            );
        }

        if (verifiedAt == null) {
            throw new IamConflictException(
                    "Unverified users cannot be reactivated directly."
            );
        }

        this.status = UserStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void changeLocale(String newLocale, Instant now) {
        Objects.requireNonNull(now, "Timestamp is required.");

        this.locale = validateLocale(newLocale);
        this.updatedAt = now;
    }

    public void updateNotificationPreferences(
            NotificationPreferences preferences,
            Instant now
    ) {
        Objects.requireNonNull(now, "Timestamp is required.");

        this.notificationPreferences = Objects.requireNonNull(
                preferences, "Notification preferences are required."
        );
        this.updatedAt = now;
    }

    private void validateInstitution() {
        boolean platformAdmin = role == Roles.PLATFORM_ADMIN;
        boolean withoutInstitution = institutionId == null;

        if (platformAdmin != withoutInstitution) {
            throw new IllegalArgumentException(
                    "Only platform administrators can have no institution."
            );
        }

        if (institutionId != null) {
            requirePositiveId(institutionId);
        }
    }

    private void validateInvitation() {
        if (invitationTokenHash != null &&
                (invitationTokenHash.isBlank() ||
                        invitationTokenHash.length() > 255)) {
            throw new IllegalArgumentException(
                    "Invalid invitation token hash."
            );
        }

        if (status == UserStatus.INVITED) {
            if (invitationTokenHash == null ||
                    invitationExpiresAt == null) {
                throw new IllegalArgumentException(
                        "Invited users require a token and expiration."
                );
            }
        }
    }

    private static String validateOptionalFullName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();

        if (normalized.isBlank() || normalized.length() > 120) {
            throw new IllegalArgumentException(
                    "Invalid full name."
            );
        }

        return normalized;
    }

    private static String validateRequiredFullName(String name) {
        String normalized = validateOptionalFullName(name);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    "Full name is required."
            );
        }

        return normalized;
    }

    private static String validateLocale(String locale) {
        if (!"es-419".equals(locale) &&
                !"en-US".equals(locale)) {
            throw new IllegalArgumentException(
                    "Unsupported locale."
            );
        }

        return locale;
    }

    private static void validateRequiredTokenHash(String tokenHash) {
        if (tokenHash == null ||
                tokenHash.isBlank() ||
                tokenHash.length() > 255) {
            throw new IllegalArgumentException(
                    "Invalid invitation token hash."
            );
        }
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


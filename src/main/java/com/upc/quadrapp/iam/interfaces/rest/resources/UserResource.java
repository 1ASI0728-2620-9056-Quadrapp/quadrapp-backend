package com.upc.quadrapp.iam.interfaces.rest.resources;

import java.time.Instant;

public record UserResource(Long id, Long institutionId, String email, String fullName, String role,
        String status, Instant verifiedAt, Instant lastLoginAt, String locale,
        NotificationPreferencesResource notificationPreferences, Instant createdAt, Instant updatedAt) {}

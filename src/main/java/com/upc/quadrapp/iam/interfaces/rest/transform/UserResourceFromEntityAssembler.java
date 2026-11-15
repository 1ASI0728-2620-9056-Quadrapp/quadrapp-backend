package com.upc.quadrapp.iam.interfaces.rest.transform;

import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.interfaces.rest.resources.NotificationPreferencesResource;
import com.upc.quadrapp.iam.interfaces.rest.resources.UserResource;

public final class UserResourceFromEntityAssembler {

    private UserResourceFromEntityAssembler() {}

    public static UserResource toResourceFromEntity(User user) {

        var preferences = user.getNotificationPreferences();

        var preferencesResource = new NotificationPreferencesResource(
                preferences.pushEnabled(),
                preferences.saturationAlerts(),
                preferences.lowArrivalAlerts(),
                preferences.staleDataAlerts(),
                preferences.eventAlerts()
        );

        return new UserResource(
                user.getId(),
                user.getInstitutionId(),
                user.getEmail().value(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getVerifiedAt(),
                user.getLastLoginAt(),
                user.getLocale(),
                preferencesResource,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

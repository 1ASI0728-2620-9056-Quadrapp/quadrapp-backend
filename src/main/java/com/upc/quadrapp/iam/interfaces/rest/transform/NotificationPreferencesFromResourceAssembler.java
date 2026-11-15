package com.upc.quadrapp.iam.interfaces.rest.transform;

import com.upc.quadrapp.iam.domain.model.valueobjects.NotificationPreferences;
import com.upc.quadrapp.iam.interfaces.rest.resources.NotificationPreferencesResource;

import java.util.Objects;

public final class NotificationPreferencesFromResourceAssembler {

    private NotificationPreferencesFromResourceAssembler() {}

    public static NotificationPreferences toValueObjectFromResource(
            NotificationPreferencesResource resource
    ) {
        Objects.requireNonNull(resource);

        return new NotificationPreferences(
                Objects.requireNonNull(resource.pushEnabled()),
                Objects.requireNonNull(resource.saturationAlerts()),
                Objects.requireNonNull(resource.lowArrivalAlerts()),
                Objects.requireNonNull(resource.staleDataAlerts()),
                Objects.requireNonNull(resource.eventAlerts())
        );
    }
}

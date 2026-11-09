package com.upc.quadrapp.iam.domain.model.valueobjects;

public record NotificationPreferences(
        boolean pushEnabled,
        boolean saturationAlerts,
        boolean lowArrivalAlerts,
        boolean staleDataAlerts,
        boolean eventAlerts
) {

    public static NotificationPreferences defaults() {
        return new NotificationPreferences(
                true,
                true,
                true,
                true,
                false
        );
    }
}

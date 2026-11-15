package com.upc.quadrapp.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesResource(@NotNull Boolean pushEnabled, @NotNull Boolean saturationAlerts,
        @NotNull Boolean lowArrivalAlerts, @NotNull Boolean staleDataAlerts, @NotNull Boolean eventAlerts) {}

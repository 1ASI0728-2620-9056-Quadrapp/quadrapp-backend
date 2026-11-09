package com.upc.quadrapp.iam.domain.model.commands;

import com.upc.quadrapp.iam.domain.model.valueobjects.NotificationPreferences;

public record UpdateNotificationPreferencesCommand(Long userId, NotificationPreferences preferences) {}


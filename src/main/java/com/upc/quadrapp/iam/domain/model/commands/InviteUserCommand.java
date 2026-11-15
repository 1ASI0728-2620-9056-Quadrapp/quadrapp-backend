package com.upc.quadrapp.iam.domain.model.commands;

import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;

public record InviteUserCommand(Long institutionId, Long invitedByUserId, String email,
        Roles role) {}


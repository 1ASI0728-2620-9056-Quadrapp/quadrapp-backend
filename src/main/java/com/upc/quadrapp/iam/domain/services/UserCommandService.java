package com.upc.quadrapp.iam.domain.services;

import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.commands.InviteUserCommand;
import com.upc.quadrapp.iam.domain.model.commands.AcceptInvitationCommand;
import com.upc.quadrapp.iam.domain.model.commands.UpdateNotificationPreferencesCommand;

public interface UserCommandService {

    User handle(InviteUserCommand command);

    User handle(AcceptInvitationCommand command);

    User handle(UpdateNotificationPreferencesCommand command);
}


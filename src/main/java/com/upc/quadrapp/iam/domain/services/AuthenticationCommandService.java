package com.upc.quadrapp.iam.domain.services;

import com.upc.quadrapp.iam.domain.model.commands.RequestOtpCommand;
import com.upc.quadrapp.iam.domain.model.commands.SignInCommand;
import com.upc.quadrapp.iam.domain.model.commands.RefreshSessionCommand;
import com.upc.quadrapp.iam.domain.model.commands.SignOutCommand;

public interface AuthenticationCommandService {

    void handle(RequestOtpCommand command);

    AuthenticationResult handle(SignInCommand command);

    AuthenticationResult handle(RefreshSessionCommand command);

    void handle(SignOutCommand command);
}


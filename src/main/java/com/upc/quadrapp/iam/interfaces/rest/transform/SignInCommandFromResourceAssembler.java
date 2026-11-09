package com.upc.quadrapp.iam.interfaces.rest.transform;

import com.upc.quadrapp.iam.domain.model.commands.SignInCommand;
import com.upc.quadrapp.iam.interfaces.rest.resources.SignInResource;

public final class SignInCommandFromResourceAssembler {

    private SignInCommandFromResourceAssembler() {}

    public static SignInCommand toCommandFromResource(
            SignInResource resource
    ) {
        return new SignInCommand(
                resource.email(),
                resource.otpCode()
        );
    }
}

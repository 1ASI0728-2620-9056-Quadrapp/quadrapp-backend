package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.domain.model.commands.RefreshSessionCommand;
import com.upc.quadrapp.iam.domain.model.commands.RequestOtpCommand;
import com.upc.quadrapp.iam.domain.model.commands.SignOutCommand;
import com.upc.quadrapp.iam.domain.services.AuthenticationCommandService;

import com.upc.quadrapp.iam.interfaces.rest.resources.*;
import com.upc.quadrapp.iam.interfaces.rest.transform.AuthenticatedUserResourceFromResultAssembler;
import com.upc.quadrapp.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/iam/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "OTP authentication and session management")
public class AuthenticationController {

    private final AuthenticationCommandService authenticationCommandService;

    @Operation(
            operationId = "requestOtp",
            summary = "Request a one-time sign-in code",
            description = "Public. Sends a one-time code when the email is authorized and always answers 202 "
                    + "with the same message, so it does not reveal which emails exist; 400 for an invalid "
                    + "email and 429 when too many codes were requested."
    )
    @PostMapping(
            value = "/request-otp",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResource> requestOtp(
            @Valid @RequestBody RequestOtpResource resource
    ) {
        authenticationCommandService.handle(
                new RequestOtpCommand(resource.email())
        );

        return ResponseEntity.accepted().body(
                new MessageResource(
                        "Si el correo está autorizado, recibirás un código de acceso."
                )
        );
    }

    @Operation(
            operationId = "signIn",
            summary = "Sign in with a one-time code",
            description = "Public. Exchanges the email and its one-time code for an access token and a refresh "
                    + "token (200); 400 for invalid data and 401 when the code is wrong or expired."
    )
    @PostMapping(
            value = "/sign-in",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthenticatedUserResource> signIn(
            @Valid @RequestBody SignInResource resource
    ) {
        var command =
                SignInCommandFromResourceAssembler
                        .toCommandFromResource(resource);

        var result = authenticationCommandService.handle(command);

        return ResponseEntity.ok(
                AuthenticatedUserResourceFromResultAssembler
                        .toResourceFromResult(result)
        );
    }

    @Operation(
            operationId = "refreshSession",
            summary = "Refresh the session",
            description = "Public; the refresh token is the credential. Returns a new access token and refresh "
                    + "token (200); 400 for invalid data and 401 when the refresh token is invalid, expired "
                    + "or revoked."
    )
    @PostMapping(
            value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthenticatedUserResource> refresh(
            @Valid @RequestBody RefreshSessionResource resource
    ) {
        var result = authenticationCommandService.handle(
                new RefreshSessionCommand(resource.refreshToken())
        );

        return ResponseEntity.ok(
                AuthenticatedUserResourceFromResultAssembler
                        .toResourceFromResult(result)
        );
    }

    @Operation(
            operationId = "signOut",
            summary = "Sign out",
            description = "Public; the refresh token is the credential. Revokes the refresh token, if it is "
                    + "still valid, and answers 204; 400 for invalid data."
    )
    @PostMapping(
            value = "/sign-out",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> signOut(
            @Valid @RequestBody SignOutResource resource
    ) {
        authenticationCommandService.handle(
                new SignOutCommand(resource.refreshToken())
        );

        return ResponseEntity.noContent().build();
    }
}

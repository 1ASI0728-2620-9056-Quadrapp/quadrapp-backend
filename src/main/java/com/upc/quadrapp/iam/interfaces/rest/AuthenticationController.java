package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.domain.model.commands.RefreshSessionCommand;
import com.upc.quadrapp.iam.domain.model.commands.RequestOtpCommand;
import com.upc.quadrapp.iam.domain.model.commands.SignOutCommand;
import com.upc.quadrapp.iam.domain.services.AuthenticationCommandService;

import com.upc.quadrapp.iam.interfaces.rest.resources.*;
import com.upc.quadrapp.iam.interfaces.rest.transform.AuthenticatedUserResourceFromResultAssembler;
import com.upc.quadrapp.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;

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

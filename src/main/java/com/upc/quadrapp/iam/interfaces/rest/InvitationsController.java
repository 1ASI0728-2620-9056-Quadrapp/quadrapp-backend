package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;

import com.upc.quadrapp.iam.domain.model.commands.InviteUserCommand;
import com.upc.quadrapp.iam.domain.services.UserCommandService;

import com.upc.quadrapp.iam.interfaces.rest.resources.InviteUserResource;
import com.upc.quadrapp.iam.interfaces.rest.resources.UserResource;
import com.upc.quadrapp.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/iam/invitations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Invitations", description = "Institutional user invitations")
public class InvitationsController {

    private final UserCommandService userCommandService;
    private final CurrentUserService currentUserService;


    @Operation(
            operationId = "inviteUser",
            summary = "Invite a user to an institution",
            description = "Administrators of the institution and platform administrators. Creates a pending "
                    + "user with the given role and issues its invitation (201); 400 when the data is "
                    + "invalid or the email domain is not registered or verified for the institution, 403 "
                    + "for other users and 409 when the email is already registered or the institution is "
                    + "not active."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResource> invite(
            @Valid @RequestBody InviteUserResource resource
    ) {
        Long actorId = currentUserService.requireAuthenticatedUserId();

        var user = userCommandService.handle(
                new InviteUserCommand(
                        resource.institutionId(),
                        actorId,
                        resource.email(),
                        resource.role()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        UserResourceFromEntityAssembler
                                .toResourceFromEntity(user)
                );
    }
}

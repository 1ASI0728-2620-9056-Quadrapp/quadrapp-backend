package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;

import com.upc.quadrapp.iam.domain.model.commands.AcceptInvitationCommand;
import com.upc.quadrapp.iam.domain.model.commands.InviteUserCommand;
import com.upc.quadrapp.iam.domain.services.UserCommandService;

import com.upc.quadrapp.iam.interfaces.rest.resources.AcceptInvitationResource;
import com.upc.quadrapp.iam.interfaces.rest.resources.InviteUserResource;
import com.upc.quadrapp.iam.interfaces.rest.resources.UserResource;
import com.upc.quadrapp.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/iam/invitations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Invitations", description = "Institutional user invitations")
public class InvitationsController {

    private final UserCommandService userCommandService;
    private final CurrentUserService currentUserService;


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

    @PostMapping(
            value = "/accept",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResource> accept(
            @Valid @RequestBody AcceptInvitationResource resource
    ) {
        var user = userCommandService.handle(
                new AcceptInvitationCommand(
                        resource.invitationToken(),
                        resource.fullName()
                )
        );

        return ResponseEntity.ok(
                UserResourceFromEntityAssembler
                        .toResourceFromEntity(user)
        );
    }
}

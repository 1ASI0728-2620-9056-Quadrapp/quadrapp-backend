package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.domain.model.commands.AcceptInvitationCommand;
import com.upc.quadrapp.iam.domain.services.UserCommandService;

import com.upc.quadrapp.iam.interfaces.rest.resources.AcceptInvitationResource;
import com.upc.quadrapp.iam.interfaces.rest.resources.UserResource;
import com.upc.quadrapp.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Accepting an invitation creates an invitation acceptance. It is public: the invited user has
 * no session yet and proves the invitation with its token.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/iam/invitation-acceptances", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Invitations", description = "Institutional user invitations")
public class InvitationAcceptancesController {

    private final UserCommandService userCommandService;

    @Operation(
            operationId = "acceptInvitation",
            summary = "Accept an invitation",
            description = "Public; the invitation token is the credential. Activates the invited user with its "
                    + "full name and returns it (200); 400 for invalid data or an unknown token and 409 "
                    + "when the institution is not active or its email domain is no longer verified."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

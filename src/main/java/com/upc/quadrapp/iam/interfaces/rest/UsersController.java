package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;

import com.upc.quadrapp.iam.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByEmailQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.quadrapp.iam.domain.services.UserCommandService;
import com.upc.quadrapp.iam.domain.services.UserQueryService;

import com.upc.quadrapp.iam.interfaces.rest.resources.NotificationPreferencesResource;
import com.upc.quadrapp.iam.interfaces.rest.resources.UserResource;
import com.upc.quadrapp.iam.interfaces.rest.transform.NotificationPreferencesFromResourceAssembler;
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
import org.springframework.web.server.ResponseStatusException;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/iam/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Institutional user queries and preferences")
public class UsersController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public ResponseEntity<UserResource> getCurrentUser() {

        Long userId = currentUserService.requireAuthenticatedUserId();

        var user = userQueryService.handle(
                new GetUserByIdQuery(userId)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        return ResponseEntity.ok(
                UserResourceFromEntityAssembler.toResourceFromEntity(user)
        );
    }

    @GetMapping("/{userId:\\d+}")
    public ResponseEntity<UserResource> getById(
            @PathVariable Long userId
    ) {
        var user = userQueryService.handle(
                new GetUserByIdQuery(userId)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        return ResponseEntity.ok(
                UserResourceFromEntityAssembler.toResourceFromEntity(user)
        );
    }

    @Operation(summary = "Find the user with this email (required filter); 404 when there is none "
            + "or it belongs to another institution")
    @GetMapping
    public ResponseEntity<UserResource> getByEmail(
            @RequestParam String email
    ) {
        var user = userQueryService.handle(
                new GetUserByEmailQuery(email)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        return ResponseEntity.ok(
                UserResourceFromEntityAssembler.toResourceFromEntity(user)
        );
    }

    @PutMapping(
            value = "/{userId}/notification-preferences",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResource> updateNotificationPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationPreferencesResource resource
    ) {
        var preferences =
                NotificationPreferencesFromResourceAssembler
                        .toValueObjectFromResource(resource);

        var user = userCommandService.handle(
                new UpdateNotificationPreferencesCommand(
                        userId,
                        preferences
                )
        );

        return ResponseEntity.ok(
                UserResourceFromEntityAssembler.toResourceFromEntity(user)
        );
    }
}

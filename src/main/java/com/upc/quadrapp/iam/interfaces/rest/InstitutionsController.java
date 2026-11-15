package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;

import com.upc.quadrapp.iam.domain.model.commands.AddInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.model.commands.CreateInstitutionCommand;
import com.upc.quadrapp.iam.domain.model.commands.VerifyInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.model.queries.GetInstitutionByIdQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetInstitutionDomainsQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUsersByInstitutionQuery;
import com.upc.quadrapp.iam.domain.services.InstitutionCommandService;
import com.upc.quadrapp.iam.domain.services.InstitutionQueryService;
import com.upc.quadrapp.iam.domain.services.UserQueryService;

import com.upc.quadrapp.iam.interfaces.rest.resources.*;
import com.upc.quadrapp.iam.interfaces.rest.transform.InstitutionDomainResourceFromEntityAssembler;
import com.upc.quadrapp.iam.interfaces.rest.transform.InstitutionResourceFromEntityAssembler;
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

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/iam/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Institutions", description = "Institution and verified domain management")
public class InstitutionsController {

    private final InstitutionCommandService institutionCommandService;
    private final InstitutionQueryService institutionQueryService;
    private final UserQueryService userQueryService;
    private final CurrentUserService currentUserService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitutionResource> create(
            @Valid @RequestBody CreateInstitutionResource resource
    ) {
        Long actorId = currentUserService.requireAuthenticatedUserId();

        var institution = institutionCommandService.handle(
                new CreateInstitutionCommand(
                        resource.name(),
                        actorId
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        InstitutionResourceFromEntityAssembler
                                .toResourceFromEntity(institution)
                );
    }

    @GetMapping("/{institutionId}")
    public ResponseEntity<InstitutionResource> getById(
            @PathVariable Long institutionId
    ) {
        var institution = institutionQueryService.handle(
                new GetInstitutionByIdQuery(institutionId)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Institution not found."
        ));

        return ResponseEntity.ok(
                InstitutionResourceFromEntityAssembler
                        .toResourceFromEntity(institution)
        );
    }

    @PostMapping(
            value = "/{institutionId}/domains",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<InstitutionDomainResource> addDomain(
            @PathVariable Long institutionId,
            @Valid @RequestBody AddInstitutionDomainResource resource
    ) {
        Long actorId = currentUserService.requireAuthenticatedUserId();

        var domain = institutionCommandService.handle(
                new AddInstitutionDomainCommand(
                        institutionId,
                        resource.domain(),
                        actorId
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        InstitutionDomainResourceFromEntityAssembler
                                .toResourceFromEntity(domain)
                );
    }

    @GetMapping("/{institutionId}/domains")
    public ResponseEntity<List<InstitutionDomainResource>> getDomains(
            @PathVariable Long institutionId
    ) {
        var domains = institutionQueryService.handle(
                new GetInstitutionDomainsQuery(institutionId)
        );

        var resources = domains.stream()
                .map(
                        InstitutionDomainResourceFromEntityAssembler
                                ::toResourceFromEntity
                )
                .toList();

        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "List the users of the institution (its admins and platform admins)")
    @GetMapping("/{institutionId}/users")
    public ResponseEntity<List<UserResource>> getUsers(
            @PathVariable Long institutionId
    ) {
        var users = userQueryService.handle(
                new GetUsersByInstitutionQuery(institutionId)
        );

        var resources = users.stream()
                .map(
                        UserResourceFromEntityAssembler
                                ::toResourceFromEntity
                )
                .toList();

        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Verify a domain of the institution with {\"status\": \"VERIFIED\"} (platform admins "
            + "only); any other status is 400 and a domain of another institution is 404")
    @PatchMapping(
            value = "/{institutionId}/domains/{domainId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<InstitutionDomainResource> updateDomain(
            @PathVariable Long institutionId,
            @PathVariable Long domainId,
            @Valid @RequestBody UpdateInstitutionDomainResource resource
    ) {
        boolean ofThisInstitution = institutionQueryService.handle(
                new GetInstitutionDomainsQuery(institutionId)
        ).stream().anyMatch(domain -> domain.getId().equals(domainId));

        if (!ofThisInstitution) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Institution domain not found."
            );
        }

        Long actorId = currentUserService.requireAuthenticatedUserId();

        var domain = institutionCommandService.handle(
                new VerifyInstitutionDomainCommand(
                        domainId,
                        actorId
                )
        );

        return ResponseEntity.ok(
                InstitutionDomainResourceFromEntityAssembler
                        .toResourceFromEntity(domain)
        );
    }
}

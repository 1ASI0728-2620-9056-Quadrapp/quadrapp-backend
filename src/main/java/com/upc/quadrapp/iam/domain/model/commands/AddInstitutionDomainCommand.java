package com.upc.quadrapp.iam.domain.model.commands;

public record AddInstitutionDomainCommand(Long institutionId, String domain, Long requestedByUserId) {}


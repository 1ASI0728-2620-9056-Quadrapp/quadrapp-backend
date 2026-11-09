package com.upc.quadrapp.iam.domain.services;

import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.domain.model.commands.CreateInstitutionCommand;
import com.upc.quadrapp.iam.domain.model.commands.AddInstitutionDomainCommand;
import com.upc.quadrapp.iam.domain.model.commands.VerifyInstitutionDomainCommand;

public interface InstitutionCommandService {

    Institution handle(CreateInstitutionCommand command);

    InstitutionDomain handle(AddInstitutionDomainCommand command);

    InstitutionDomain handle(VerifyInstitutionDomainCommand command);
}


package com.upc.quadrapp.iam.domain.services;

import com.upc.quadrapp.iam.domain.model.aggregates.Institution;
import com.upc.quadrapp.iam.domain.model.entities.InstitutionDomain;
import com.upc.quadrapp.iam.domain.model.queries.GetInstitutionByIdQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetInstitutionDomainsQuery;

import java.util.List;
import java.util.Optional;

public interface InstitutionQueryService {

    Optional<Institution> handle(GetInstitutionByIdQuery query);

    List<InstitutionDomain> handle(GetInstitutionDomainsQuery query);
}


package com.upc.quadrapp.iam.domain.services;

import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUserByEmailQuery;
import com.upc.quadrapp.iam.domain.model.queries.GetUsersByInstitutionQuery;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {

    Optional<User> handle(GetUserByIdQuery query);

    Optional<User> handle(GetUserByEmailQuery query);

    List<User> handle(GetUsersByInstitutionQuery query);
}


package com.upc.quadrapp.iam.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.iam.infrastructure.persistence.jpa.entities.InstitutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionJpaRepository extends JpaRepository<InstitutionJpaEntity, Long> {
    boolean existsByName(String name);
}

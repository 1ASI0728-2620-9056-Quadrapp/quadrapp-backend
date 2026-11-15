package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories;

import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.entities.CalendarEventJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CalendarEventJpaRepository
        extends JpaRepository<CalendarEventJpaEntity, Long> {

    @Query("""
            SELECT event
            FROM CalendarEventJpaEntity event
            WHERE event.institutionId = :institutionId
              AND event.startAt < :to
              AND event.endAt > :from
            ORDER BY event.startAt ASC
            """)
    List<CalendarEventJpaEntity> findOverlappingByInstitutionId(
            @Param("institutionId") Long institutionId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}

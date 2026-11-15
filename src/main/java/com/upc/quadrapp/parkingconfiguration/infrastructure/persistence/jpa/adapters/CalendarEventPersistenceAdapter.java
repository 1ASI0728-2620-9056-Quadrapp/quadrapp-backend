package com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.adapters;

import com.upc.quadrapp.parkingconfiguration.application.internal.outboundservices.persistence.CalendarEventPersistencePort;
import com.upc.quadrapp.parkingconfiguration.domain.model.aggregates.CalendarEvent;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.mappers.ParkingConfigurationJpaMapper;
import com.upc.quadrapp.parkingconfiguration.infrastructure.persistence.jpa.repositories.CalendarEventJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CalendarEventPersistenceAdapter
        implements CalendarEventPersistencePort {

    private final CalendarEventJpaRepository repository;
    private final ParkingConfigurationJpaMapper mapper;

    @Override
    public CalendarEvent save(CalendarEvent value) {
        return mapper.toDomain(
                repository.save(mapper.toJpa(value))
        );
    }

    @Override
    public Optional<CalendarEvent> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<CalendarEvent> findOverlappingByInstitutionId(
            Long institutionId,
            Instant from,
            Instant to
    ) {
        return repository.findOverlappingByInstitutionId(
                        institutionId,
                        from,
                        to
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

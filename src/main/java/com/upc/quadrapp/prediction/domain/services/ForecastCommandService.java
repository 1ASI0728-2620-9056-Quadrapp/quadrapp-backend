package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.commands.GenerateForecastsCommand;

import java.time.Instant;
import java.util.List;

public interface ForecastCommandService {

    /**
     * Returns the scheduled run of the current 5-minute slot of a parking lot of the user's
     * institution, generating it first when it does not exist yet.
     */
    List<Forecast> handle(GenerateForecastsCommand command);

    /** Generates the scheduled run of the current slot for every active parking lot. */
    int generateScheduledRuns();

    /**
     * Generates a CAMPUS_EVENT run for the affected parking lots when the event window overlaps
     * the next 60 minutes. A null {@code parkingLotId} means every lot of the institution.
     */
    int regenerateForCampusEvent(Long institutionId, Long parkingLotId, Instant startAt, Instant endAt);

    int evaluateDueForecasts();

    /** Deletes the forecasts generated more than 7 days ago. */
    int purgeExpiredForecasts();
}

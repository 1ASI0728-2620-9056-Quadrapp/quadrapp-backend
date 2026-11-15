package com.upc.quadrapp.prediction.interfaces.events;

import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventCancelled;
import com.upc.quadrapp.parkingconfiguration.domain.model.events.CampusEventProvided;
import com.upc.quadrapp.prediction.domain.services.ForecastCommandService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Consumer of the campus events of Parking Configuration (US35). Registering or cancelling an
 * event that overlaps the next 60 minutes regenerates the forecasts of the affected parking lots.
 * Each event arrives after the Configuration transaction commits, in a new transaction.
 */
@Component
public class CampusEventListener {

    private final ForecastCommandService forecasts;

    public CampusEventListener(ForecastCommandService forecasts) {
        this.forecasts = forecasts;
    }

    @ApplicationModuleListener
    public void on(CampusEventProvided event) {
        forecasts.regenerateForCampusEvent(event.tenantId(), event.parkingLotId(), event.startAt(), event.endAt());
    }

    @ApplicationModuleListener
    public void on(CampusEventCancelled event) {
        forecasts.regenerateForCampusEvent(event.tenantId(), event.parkingLotId(), event.startAt(), event.endAt());
    }
}

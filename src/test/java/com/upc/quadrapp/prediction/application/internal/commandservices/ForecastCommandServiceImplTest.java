package com.upc.quadrapp.prediction.application.internal.commandservices;

import com.upc.quadrapp.iam.interfaces.acl.IamContextFacade;
import com.upc.quadrapp.occupancy.interfaces.acl.OccupancyContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.prediction.application.internal.outboundservices.persistence.ForecastPersistencePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ConfidenceLevel;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ForecastMethod;
import com.upc.quadrapp.prediction.domain.services.OccupancyForecastingModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ForecastCommandServiceImplTest {

    private final ForecastPersistencePort forecasts = mock(ForecastPersistencePort.class);
    private final OccupancyContextFacade occupancy = mock(OccupancyContextFacade.class);
    private final ParkingConfigurationContextFacade parkingConfiguration =
            mock(ParkingConfigurationContextFacade.class);
    private final IamContextFacade iam = mock(IamContextFacade.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-01-01T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void dueForecastsAreEvaluatedAgainstKnownOccupancyOnly() {
        Forecast due = Forecast.create(
                1L,
                15,
                Instant.parse("2026-01-01T09:00:00Z"),
                new BigDecimal("50.00"),
                new BigDecimal("0.500"),
                ConfidenceLevel.MEDIUM,
                ForecastMethod.PRIMARY_MODEL,
                "linear-trend-v1",
                null
        );
        when(forecasts.findDueUnobserved(any(), anyInt())).thenReturn(List.of(due));
        when(occupancy.currentOccupancyForInternalUse(1L))
                .thenReturn(new OccupancyContextFacade.OccupancyView(
                        "PARKING_LOT",
                        1L,
                        clock.instant(),
                        100,
                        20,
                        60,
                        20
                ));

        ForecastCommandServiceImpl service = new ForecastCommandServiceImpl(
                forecasts,
                occupancy,
                parkingConfiguration,
                iam,
                new OccupancyForecastingModel(),
                clock
        );

        int evaluated = service.evaluateDueForecasts();

        assertEquals(1, evaluated);
        verify(forecasts).save(argThat(saved ->
                new BigDecimal("75.00").equals(saved.observedOccupancyPct())
                        && new BigDecimal("25.00").equals(saved.absoluteError())
        ));
    }
}

package com.upc.quadrapp.parkingconfiguration.application.internal;

import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CreateLevelCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CreateSpaceCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CreateZoneCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.exceptions.ParkingConfigurationNotFoundException;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetLevelsByZoneQuery;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetSpacesByLevelQuery;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetZonesByParkingLotQuery;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceType;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A parking lot, zone or level named in the path that does not exist is "not found" (404),
 * not a bad request.
 */
@QuadrappIntegrationTest
class MissingConfigurationResourceIntegrationTest {

    private static final long MISSING = 987_654_321L;

    @Autowired
    private ParkingConfigurationQueryService queries;

    @Autowired
    private ParkingConfigurationCommandService commands;

    @Test
    void missingParentsOfQueriesAreNotFound() {
        assertThatThrownBy(() -> queries.handle(new GetZonesByParkingLotQuery(MISSING)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Parking lot not found.");
        assertThatThrownBy(() -> queries.handle(new GetLevelsByZoneQuery(MISSING)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Zone not found.");
        assertThatThrownBy(() -> queries.handle(new GetSpacesByLevelQuery(MISSING)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Level not found.");
    }

    @Test
    void missingParentsOfCommandsAreNotFound() {
        assertThatThrownBy(() -> commands.handle(new CreateZoneCommand(MISSING, "North", null, null)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Parking lot not found.");
        assertThatThrownBy(() -> commands.handle(new CreateLevelCommand(MISSING, "L1", null)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Zone not found.");
        assertThatThrownBy(() -> commands.handle(new CreateSpaceCommand(MISSING, "A1", SpaceType.values()[0],
                BigDecimal.ONE, BigDecimal.ONE)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Level not found.");
    }
}

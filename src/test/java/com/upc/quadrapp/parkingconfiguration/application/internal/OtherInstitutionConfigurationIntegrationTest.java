package com.upc.quadrapp.parkingconfiguration.application.internal;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.parkingconfiguration.domain.model.commands.CreateZoneCommand;
import com.upc.quadrapp.parkingconfiguration.domain.model.exceptions.ParkingConfigurationNotFoundException;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetLevelsByZoneQuery;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetParkingLotByIdQuery;
import com.upc.quadrapp.parkingconfiguration.domain.model.queries.GetZonesByParkingLotQuery;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationCommandService;
import com.upc.quadrapp.parkingconfiguration.domain.services.ParkingConfigurationQueryService;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Parking lots, zones and levels of another institution answer exactly like missing ones, so a
 * caller cannot discover which IDs exist in other tenants. Inside the own institution, a missing
 * write permission is still 403.
 */
@QuadrappIntegrationTest
class OtherInstitutionConfigurationIntegrationTest {

    @Autowired
    private ParkingConfigurationQueryService queries;

    @Autowired
    private ParkingConfigurationCommandService commands;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserService currentUser;

    private long otherLot;
    private long otherZone;
    private long ownLot;
    private long ownAdmin;
    private long ownOperator;

    @BeforeEach
    void twoInstitutions() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long own = institution("Own " + suffix);
        long other = institution("Other " + suffix);
        ownAdmin = user(own, "ADMIN", "admin@own-" + suffix + ".test");
        ownOperator = user(own, "OPERATOR", "ops@own-" + suffix + ".test");
        ownLot = lot(own, "Own lot " + suffix);
        otherLot = lot(other, "Other lot " + suffix);
        otherZone = jdbc.queryForObject("insert into configuration.zones (lot_id, name, sort_order, created_at, updated_at) "
                + "values (?, 'North', 0, now(), now()) returning id", Long.class, otherLot);
    }

    @Test
    void resourcesOfAnotherInstitutionLookMissing() {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(ownAdmin);

        assertThat(queries.handle(new GetParkingLotByIdQuery(otherLot))).isEmpty();
        assertThatThrownBy(() -> queries.handle(new GetZonesByParkingLotQuery(otherLot)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Parking lot not found.");
        assertThatThrownBy(() -> queries.handle(new GetLevelsByZoneQuery(otherZone)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Zone not found.");
        assertThatThrownBy(() -> commands.handle(new CreateZoneCommand(otherLot, "South", null, null)))
                .isInstanceOf(ParkingConfigurationNotFoundException.class).hasMessage("Parking lot not found.");
    }

    @Test
    void anOperatorOfTheSameInstitutionStillGets403OnWrites() {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(ownOperator);

        assertThat(queries.handle(new GetZonesByParkingLotQuery(ownLot))).isEmpty();
        assertThatThrownBy(() -> commands.handle(new CreateZoneCommand(ownLot, "South", null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private long institution(String name) {
        return jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id", Long.class, name);
    }

    private long user(long institutionId, String role, String email) {
        return jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, ?, ?, 'ACTIVE') returning id", Long.class, institutionId, role, email);
    }

    private long lot(long institutionId, String name) {
        return jdbc.queryForObject("insert into configuration.parking_lots (accuracy_margin_pct, active, "
                + "flow_window_minutes, freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name) "
                + "values (5, true, 15, 10, 1, 20, 10, 90, 300, now(), ?, now(), ?) returning id",
                Long.class, institutionId, name);
    }
}

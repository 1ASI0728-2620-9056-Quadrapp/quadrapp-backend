package com.upc.quadrapp.parkingconfiguration.interfaces.acl;

import com.upc.quadrapp.iam.application.internal.outboundservices.security.CurrentUserService;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.AccessLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.LevelLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.ParkingLotView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.SpaceLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.ZoneLayoutView;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Occupancy keeps its own copy of the layout and reads it only through this facade, never with
 * SQL against the configuration schema. Lookups for the REST queries treat resources of another
 * institution exactly like missing ones.
 */
@QuadrappIntegrationTest
class ParkingLayoutFacadeIntegrationTest {

    @Autowired
    private ParkingConfigurationContextFacade facade;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserService currentUser;

    private long ownUser;
    private long ownLot;
    private long zone;
    private long level;
    private long activeSpace;
    private long inactiveSpace;
    private long access;
    private long otherLot;
    private long otherZone;
    private long otherLevel;

    @BeforeEach
    void twoInstitutions() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long own = jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id",
                Long.class, "Own " + suffix);
        long other = jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id",
                Long.class, "Other " + suffix);
        ownUser = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'OPERATOR', ?, 'ACTIVE') returning id", Long.class, own, "ops@" + suffix + ".test");
        ownLot = lot(own, "Own " + suffix, 15);
        zone = zone(ownLot);
        level = level(zone);
        activeSpace = space(level, "A-1", true);
        inactiveSpace = space(level, "A-2", false);
        access = jdbc.queryForObject("insert into configuration.accesses (lot_id, name, type, flow_counting_enabled, "
                + "created_at, updated_at) values (?, 'Gate', 'ENTRY', true, now(), now()) returning id",
                Long.class, ownLot);
        otherLot = lot(other, "Other " + suffix, 10);
        otherZone = zone(otherLot);
        otherLevel = level(otherZone);
    }

    @Test
    void layoutListsZonesLevelsSpacesAccessesAndTheThreshold() {
        var layout = facade.layoutOf(ownLot).orElseThrow();

        assertThat(layout.institutionId()).isNotNull();
        assertThat(layout.lowAvailabilityPct()).isEqualTo(15);
        assertThat(layout.zones()).containsExactly(new ZoneLayoutView(zone, List.of(new LevelLayoutView(level,
                List.of(new SpaceLayoutView(activeSpace, true), new SpaceLayoutView(inactiveSpace, false))))));
        assertThat(layout.accesses()).containsExactly(new AccessLayoutView(access, true));
        assertThat(facade.layoutOf(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void lotsZonesAndLevelsOfAnotherInstitutionLookMissing() {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(ownUser);

        assertThat(facade.findReadableParkingLot(ownLot)).map(ParkingLotView::id).contains(ownLot);
        assertThat(facade.findReadableParkingLotOfZone(zone)).map(ParkingLotView::id).contains(ownLot);
        assertThat(facade.findReadableParkingLotOfLevel(level)).map(ParkingLotView::id).contains(ownLot);
        assertThat(facade.findReadableParkingLot(otherLot)).isEmpty();
        assertThat(facade.findReadableParkingLotOfZone(otherZone)).isEmpty();
        assertThat(facade.findReadableParkingLotOfLevel(otherLevel)).isEmpty();
        assertThat(facade.findReadableParkingLotOfZone(Long.MAX_VALUE)).isEmpty();
    }

    private long lot(long institutionId, String name, int lowAvailabilityPct) {
        return jdbc.queryForObject("insert into configuration.parking_lots (accuracy_margin_pct, active, "
                + "flow_window_minutes, freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name, "
                + "low_availability_pct) values (5, true, 15, 10, 1, 20, 10, 90, 300, now(), ?, now(), ?, ?) "
                + "returning id", Long.class, institutionId, name, lowAvailabilityPct);
    }

    private long zone(long lotId) {
        return jdbc.queryForObject("insert into configuration.zones (lot_id, name, sort_order, created_at, updated_at) "
                + "values (?, 'North', 0, now(), now()) returning id", Long.class, lotId);
    }

    private long level(long zoneId) {
        return jdbc.queryForObject("insert into configuration.levels (zone_id, name, sort_order, created_at, updated_at) "
                + "values (?, 'L1', 0, now(), now()) returning id", Long.class, zoneId);
    }

    private long space(long levelId, String label, boolean active) {
        return jdbc.queryForObject("insert into configuration.spaces (level_id, label, type, status, unknown_reason, "
                + "status_since, active, created_at, updated_at) values (?, ?, 'STANDARD', 'UNKNOWN', 'NO_SENSOR', "
                + "now(), ?, now(), now()) returning id", Long.class, levelId, label, active);
    }
}

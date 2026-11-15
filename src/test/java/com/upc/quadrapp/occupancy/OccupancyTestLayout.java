package com.upc.quadrapp.occupancy;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates an institution with one parking lot (one zone, one level, active spaces, one inactive
 * space and one access) directly in the configuration tables, as Parking Configuration would.
 */
public final class OccupancyTestLayout {

    public final long institutionId;
    public final long userId;
    public final long parkingLotId;
    public final long zoneId;
    public final long levelId;
    public final List<Long> spaceIds;
    public final long inactiveSpaceId;
    public final long accessId;

    private OccupancyTestLayout(long institutionId, long userId, long parkingLotId, long zoneId, long levelId,
                                List<Long> spaceIds, long inactiveSpaceId, long accessId) {
        this.institutionId = institutionId;
        this.userId = userId;
        this.parkingLotId = parkingLotId;
        this.zoneId = zoneId;
        this.levelId = levelId;
        this.spaceIds = spaceIds;
        this.inactiveSpaceId = inactiveSpaceId;
        this.accessId = accessId;
    }

    public static OccupancyTestLayout create(JdbcTemplate jdbc, int activeSpaces, int lowAvailabilityPct) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long institution = jdbc.queryForObject("insert into iam.institutions (name) values (?) returning id",
                Long.class, "Institution " + suffix);
        long user = jdbc.queryForObject("insert into iam.users (institution_id, role, email, status) "
                + "values (?, 'OPERATOR', ?, 'ACTIVE') returning id", Long.class, institution, "ops@" + suffix + ".test");
        long lot = jdbc.queryForObject("insert into configuration.parking_lots (accuracy_margin_pct, active, "
                + "flow_window_minutes, freed_debounce_s, layout_version, low_battery_threshold_pct, occupied_debounce_s, "
                + "saturation_threshold_pct, stale_after_seconds, created_at, institution_id, updated_at, name, "
                + "low_availability_pct) values (5, true, 15, 10, 1, 20, 10, 90, 300, now(), ?, now(), ?, ?) "
                + "returning id", Long.class, institution, "Lot " + suffix, lowAvailabilityPct);
        long zone = jdbc.queryForObject("insert into configuration.zones (lot_id, name, sort_order, created_at, "
                + "updated_at) values (?, 'North', 0, now(), now()) returning id", Long.class, lot);
        long level = jdbc.queryForObject("insert into configuration.levels (zone_id, name, sort_order, created_at, "
                + "updated_at) values (?, 'L1', 0, now(), now()) returning id", Long.class, zone);
        List<Long> spaces = new ArrayList<>();
        for (int i = 1; i <= activeSpaces; i++) {
            spaces.add(space(jdbc, level, "S-" + i, true));
        }
        long inactive = space(jdbc, level, "OFF", false);
        long access = jdbc.queryForObject("insert into configuration.accesses (lot_id, name, type, "
                + "flow_counting_enabled, created_at, updated_at) values (?, 'Gate', 'ENTRY', true, now(), now()) "
                + "returning id", Long.class, lot);
        return new OccupancyTestLayout(institution, user, lot, zone, level, List.copyOf(spaces), inactive, access);
    }

    public static long space(JdbcTemplate jdbc, long levelId, String label, boolean active) {
        return jdbc.queryForObject("insert into configuration.spaces (level_id, label, type, status, unknown_reason, "
                + "status_since, active, created_at, updated_at) values (?, ?, 'STANDARD', 'UNKNOWN', 'NO_SENSOR', "
                + "now(), ?, now(), now()) returning id", Long.class, levelId, label, active);
    }

    public long space(int index) {
        return spaceIds.get(index);
    }
}

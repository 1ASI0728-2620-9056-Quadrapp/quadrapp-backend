-- Occupancy owns the state of spaces and parking lots (report, section 5.4). The layout comes from
-- Parking Configuration through its facade; IDs of other modules carry no foreign key (CON-02).

-- Business state of each space. Inactive spaces keep their row but never change state and are
-- not counted.
CREATE TABLE occupancy.space_occupancy (
    space_id bigint PRIMARY KEY,
    tenant_id bigint NOT NULL,
    parking_lot_id bigint NOT NULL,
    zone_id bigint NOT NULL,
    level_id bigint NOT NULL,
    active boolean NOT NULL,
    status varchar(10) NOT NULL,
    unknown_reason varchar(30),
    status_since timestamptz NOT NULL,
    last_detected_at timestamptz,
    last_event_id varchar(100),
    version bigint NOT NULL,
    CONSTRAINT ck_space_occupancy_status CHECK (status IN ('FREE', 'OCCUPIED', 'UNKNOWN')),
    CONSTRAINT ck_space_occupancy_reason CHECK (
        (status = 'UNKNOWN' AND unknown_reason IN ('NOT_YET_REPORTED', 'NO_SENSOR', 'SENSOR_FAULT',
                                                   'GATEWAY_OFFLINE', 'AWAITING_STABLE_DETECTION'))
        OR (status <> 'UNKNOWN' AND unknown_reason IS NULL)
    )
);

CREATE INDEX ix_space_occupancy_lot_zone ON occupancy.space_occupancy (parking_lot_id, zone_id);
CREATE INDEX ix_space_occupancy_level ON occupancy.space_occupancy (level_id);

-- Availability of each parking lot (active spaces only) and its low-availability condition.
CREATE TABLE occupancy.lot_occupancy (
    parking_lot_id bigint PRIMARY KEY,
    tenant_id bigint NOT NULL,
    total_spaces integer NOT NULL,
    free_count integer NOT NULL,
    occupied_count integer NOT NULL,
    unknown_count integer NOT NULL,
    low_availability_pct smallint NOT NULL,
    low_availability boolean NOT NULL,
    consolidated_at timestamptz,
    layout_synced_at timestamptz NOT NULL,
    version bigint NOT NULL,
    CONSTRAINT ck_lot_occupancy_total CHECK (free_count + occupied_count + unknown_count = total_spaces),
    CONSTRAINT ck_lot_occupancy_low_availability_pct CHECK (low_availability_pct BETWEEN 5 AND 30)
);

-- Accesses of each parking lot, copied from the layout: a passage counts only through them.
CREATE TABLE occupancy.lot_access_points (
    access_point_id bigint PRIMARY KEY,
    parking_lot_id bigint NOT NULL,
    flow_counting boolean NOT NULL
);

CREATE INDEX ix_lot_access_points_lot ON occupancy.lot_access_points (parking_lot_id);

-- Vehicle passages, kept 7 days. The eventId of Parking Sensing is the key, so a passage is
-- never recorded twice. No data identifies the vehicle or the driver (CON-16).
CREATE TABLE occupancy.vehicle_passages (
    event_id varchar(100) PRIMARY KEY,
    tenant_id bigint NOT NULL,
    parking_lot_id bigint NOT NULL,
    access_point_id bigint NOT NULL,
    direction varchar(3) NOT NULL,
    passed_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL,
    CONSTRAINT ck_vehicle_passages_direction CHECK (direction IN ('IN', 'OUT'))
);

CREATE INDEX ix_vehicle_passages_lot_passed ON occupancy.vehicle_passages (parking_lot_id, passed_at);
CREATE INDEX ix_vehicle_passages_passed ON occupancy.vehicle_passages (passed_at);

-- Snapshots now carry their parking lot, so the history of a lot is read without the
-- configuration schema. Rows saved before this version have no lot and are only read by zone;
-- they leave the table with the 8-week retention.
ALTER TABLE occupancy.occupancy_snapshots ADD COLUMN parking_lot_id bigint;

CREATE INDEX ix_occupancy_snapshots_lot_taken ON occupancy.occupancy_snapshots (parking_lot_id, taken_at);
CREATE INDEX ix_occupancy_snapshots_taken ON occupancy.occupancy_snapshots (taken_at);

-- The deduplication log of sensor events is kept 7 days, like the passages.
CREATE INDEX ix_sensor_events_received ON occupancy.sensor_events (received_at);

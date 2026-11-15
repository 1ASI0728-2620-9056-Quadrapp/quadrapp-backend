-- Low-availability threshold of OperatingSettings in the report: the parking lot enters low
-- availability when its free spaces fall below this percentage (5-30 %, 10 % by default).
-- Occupancy reads it through the configuration facade; existing lots take the default.
ALTER TABLE configuration.parking_lots
    ADD COLUMN low_availability_pct smallint NOT NULL DEFAULT 10,
    ADD CONSTRAINT ck_parking_lots_low_availability CHECK (low_availability_pct BETWEEN 5 AND 30);

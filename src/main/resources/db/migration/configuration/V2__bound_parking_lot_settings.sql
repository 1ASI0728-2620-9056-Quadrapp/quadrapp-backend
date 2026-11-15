-- Operating settings follow the ranges of OperatingSettings in the report: saturation 50-100 %,
-- flow window 5-60 min, minimum detection time 3-60 s and accuracy margin 1-30 points.
-- Rows saved before these limits are brought into range first, so reading them never fails.
UPDATE configuration.parking_lots
SET saturation_threshold_pct = LEAST(GREATEST(saturation_threshold_pct, 50), 100),
    low_battery_threshold_pct = LEAST(GREATEST(low_battery_threshold_pct, 1), 100),
    flow_window_minutes = LEAST(GREATEST(flow_window_minutes, 5), 60),
    accuracy_margin_pct = LEAST(GREATEST(accuracy_margin_pct, 1), 30),
    occupied_debounce_s = LEAST(GREATEST(occupied_debounce_s, 3), 60),
    freed_debounce_s = LEAST(GREATEST(freed_debounce_s, 3), 60),
    stale_after_seconds = GREATEST(stale_after_seconds, 1);

ALTER TABLE configuration.parking_lots
    ADD CONSTRAINT ck_parking_lots_saturation_threshold CHECK (saturation_threshold_pct BETWEEN 50 AND 100),
    ADD CONSTRAINT ck_parking_lots_low_battery_threshold CHECK (low_battery_threshold_pct BETWEEN 1 AND 100),
    ADD CONSTRAINT ck_parking_lots_flow_window CHECK (flow_window_minutes BETWEEN 5 AND 60),
    ADD CONSTRAINT ck_parking_lots_accuracy_margin CHECK (accuracy_margin_pct BETWEEN 1 AND 30),
    ADD CONSTRAINT ck_parking_lots_occupied_debounce CHECK (occupied_debounce_s BETWEEN 3 AND 60),
    ADD CONSTRAINT ck_parking_lots_freed_debounce CHECK (freed_debounce_s BETWEEN 3 AND 60),
    ADD CONSTRAINT ck_parking_lots_stale_after CHECK (stale_after_seconds > 0);

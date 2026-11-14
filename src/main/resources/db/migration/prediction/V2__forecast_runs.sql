-- Forecasts are generated in runs: every 5 minutes per active parking lot (SCHEDULED) and when a
-- campus event that affects the next hour is registered or cancelled (CAMPUS_EVENT).
ALTER TABLE prediction.forecasts
    ADD COLUMN run_id uuid,
    ADD COLUMN run_slot timestamptz,
    ADD COLUMN trigger varchar(12),
    ADD COLUMN evaluated_at timestamptz;

-- Forecasts saved before runs existed: each request was its own run. They keep a null trigger.
UPDATE prediction.forecasts
SET run_id = md5(lot_id::text || '/' || generated_at::text)::uuid,
    run_slot = date_bin('5 minutes', generated_at, TIMESTAMPTZ '2000-01-01 00:00:00+00'),
    evaluated_at = CASE WHEN observed_occupancy_pct IS NOT NULL THEN target_at END;

ALTER TABLE prediction.forecasts
    ALTER COLUMN run_id SET NOT NULL,
    ALTER COLUMN run_slot SET NOT NULL,
    ADD CONSTRAINT ck_forecasts_trigger CHECK (trigger IN ('SCHEDULED', 'CAMPUS_EVENT')),
    ADD CONSTRAINT uk_forecasts_lot_horizon_run UNIQUE (lot_id, horizon_min, run_id);

-- Two replicas never save the same scheduled run twice.
CREATE UNIQUE INDEX uk_forecasts_lot_horizon_scheduled_slot
    ON prediction.forecasts (lot_id, horizon_min, run_slot)
    WHERE trigger = 'SCHEDULED';

-- Queue of forecasts waiting for evaluation (observed or marked unobservable sets evaluated_at).
CREATE INDEX ix_forecasts_unevaluated_target
    ON prediction.forecasts (target_at)
    WHERE evaluated_at IS NULL;

-- Daily purge of forecasts older than 7 days.
CREATE INDEX ix_forecasts_generated ON prediction.forecasts (generated_at);

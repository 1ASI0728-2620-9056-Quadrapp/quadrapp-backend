-- A space has at most one sensor. The application checks it before assigning, and this index
-- closes the race between two concurrent assignments. Decommissioning a sensor clears its space.
-- If a database already has two sensors on one space, this migration stops with a unique
-- violation that names the space: unassign one of them before starting the application.
CREATE UNIQUE INDEX uk_devices_space ON sensing.devices (space_id) WHERE space_id IS NOT NULL;

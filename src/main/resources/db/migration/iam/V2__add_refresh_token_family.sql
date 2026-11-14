-- Refresh token rotation by family: every sign-in starts a family and each rotation keeps it.
-- Presenting a token that was already revoked revokes the whole family (OAuth 2.0 Security BCP).
-- Existing tokens become a family each, so no session is revoked by this migration.
ALTER TABLE iam.refresh_tokens ADD COLUMN family_id uuid;
UPDATE iam.refresh_tokens SET family_id = gen_random_uuid();
ALTER TABLE iam.refresh_tokens ALTER COLUMN family_id SET NOT NULL;
CREATE INDEX idx_refresh_tokens_family_active ON iam.refresh_tokens (family_id) WHERE revoked_at IS NULL;

-- Each institution admin adds its own domains; a platform admin verifies them.
-- An unverified claim must not block the real owner, so a domain is unique only once verified,
-- and an institution cannot add the same domain twice.
ALTER TABLE iam.institution_domains DROP CONSTRAINT uk_institution_domains_domain;
ALTER TABLE iam.institution_domains
    ADD CONSTRAINT uk_institution_domains_institution_domain UNIQUE (institution_id, domain);
CREATE UNIQUE INDEX uk_institution_domains_verified_domain
    ON iam.institution_domains (domain) WHERE verified_at IS NOT NULL;

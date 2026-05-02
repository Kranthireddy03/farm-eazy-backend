-- production_add_auth_provider_and_profile_completed.sql
-- Idempotent SQL to add and populate `auth_provider` and `profile_completed` on PostgreSQL.
-- Run this as a DBA (pg_dump backup recommended) before redeploying the application.

BEGIN;

-- Add auth_provider if missing, backfill, set default and NOT NULL safely
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(50);
UPDATE users SET auth_provider = 'PASSWORD' WHERE auth_provider IS NULL OR auth_provider = '';
ALTER TABLE users ALTER COLUMN auth_provider SET DEFAULT 'PASSWORD';
UPDATE users SET auth_provider = 'PASSWORD' WHERE auth_provider IS NULL;
ALTER TABLE users ALTER COLUMN auth_provider SET NOT NULL;

-- Add profile_completed if missing, backfill, set default and NOT NULL safely
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN;
UPDATE users SET profile_completed = TRUE WHERE profile_completed IS NULL;
ALTER TABLE users ALTER COLUMN profile_completed SET DEFAULT TRUE;
UPDATE users SET profile_completed = TRUE WHERE profile_completed IS NULL;
ALTER TABLE users ALTER COLUMN profile_completed SET NOT NULL;

COMMIT;

-- Notes:
-- 1) Running this requires ALTER privileges on the `users` table.
-- 2) Test on a staging DB first. If your DB is large, consider running the UPDATEs in batches.
-- 3) If you use logical replication or strong locks are a concern, schedule a maintenance window.

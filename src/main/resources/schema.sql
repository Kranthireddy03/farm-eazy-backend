-- Idempotent startup schema fixes for PostgreSQL production profile.
-- Ensures the users table has the profile_completed column before JPA validation.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN;

UPDATE users
SET profile_completed = TRUE
WHERE profile_completed IS NULL;

ALTER TABLE users
    ALTER COLUMN profile_completed SET DEFAULT TRUE;

ALTER TABLE users
    ALTER COLUMN profile_completed SET NOT NULL;
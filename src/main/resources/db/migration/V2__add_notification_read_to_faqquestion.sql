-- Flyway migration: add notification_read to faqquestion
ALTER TABLE faqquestion ADD COLUMN IF NOT EXISTS notification_read BOOLEAN DEFAULT FALSE;
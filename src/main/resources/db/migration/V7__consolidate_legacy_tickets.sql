-- Flyway migration: consolidate legacy `tickets` tables into `support_tickets`
-- 1) Copy rows from `tickets` -> `support_tickets` (mark source='LEGACY')
INSERT INTO support_tickets (display_id, user_id, subject, description, category, priority, status, assigned_to, is_important, is_archived, sla_by, created_at, updated_at, source)
SELECT t.display_id, t.created_by, t.title, t.description, t.category, t.priority, t.status, t.assigned_to, t.is_important, t.is_archived, t.sla_by, t.created_at, t.updated_at, 'LEGACY'
FROM tickets t
WHERE NOT EXISTS (SELECT 1 FROM support_tickets s WHERE s.display_id = t.display_id);

-- 2) Migrate ticket messages -> support_ticket_messages
INSERT INTO support_ticket_messages (support_ticket_id, sender_type, sender_name, message, attachment_url, created_at)
SELECT s.id,
       CASE WHEN tm.sender_id IS NULL THEN 'SYSTEM' ELSE 'USER' END,
       NULL,
       tm.message,
       tm.attachment_url,
       tm.created_at
FROM ticket_messages tm
JOIN tickets t ON t.id = tm.ticket_id
JOIN support_tickets s ON s.display_id = t.display_id
WHERE NOT EXISTS (
  SELECT 1 FROM support_ticket_messages m
  WHERE m.support_ticket_id = s.id AND m.message = tm.message AND m.created_at = tm.created_at
);

-- 3) For existing ticket attachments, create a synthetic system message per ticket (if not present), then attach files to it
-- Create the synthetic message (one per ticket with attachments)
INSERT INTO support_ticket_messages (support_ticket_id, sender_type, sender_name, message, attachment_url, created_at)
SELECT DISTINCT s.id, 'SYSTEM', 'migration', 'Migrated attachments', NULL, ta.created_at
FROM ticket_attachments ta
JOIN tickets t ON t.id = ta.ticket_id
JOIN support_tickets s ON s.display_id = t.display_id
WHERE NOT EXISTS (
  SELECT 1 FROM support_ticket_messages m WHERE m.support_ticket_id = s.id AND m.sender_type = 'SYSTEM' AND m.message = 'Migrated attachments'
);

-- 4) Insert ticket_attachments into support_ticket_attachments linked to the synthetic message
INSERT INTO support_ticket_attachments (support_ticket_message_id, file_name, url, created_at)
SELECT
  (SELECT m.id FROM support_ticket_messages m WHERE m.support_ticket_id = s.id AND m.message = 'Migrated attachments' ORDER BY m.created_at LIMIT 1) AS msg_id,
  NULLIF(SUBSTRING_INDEX(SUBSTRING_INDEX(ta.file_url, '?', 1), '/', -1), '') AS file_name,
  ta.file_url AS url,
  ta.created_at
FROM ticket_attachments ta
JOIN tickets t ON t.id = ta.ticket_id
JOIN support_tickets s ON s.display_id = t.display_id
WHERE NOT EXISTS (
  SELECT 1 FROM support_ticket_attachments a WHERE a.url = ta.file_url AND a.support_ticket_message_id = (
    SELECT m.id FROM support_ticket_messages m WHERE m.support_ticket_id = s.id AND m.message = 'Migrated attachments' ORDER BY m.created_at LIMIT 1
  )
);

-- 5) Drop legacy ticket tables (they were migrated). Keep this as the final step.
DROP TABLE IF EXISTS ticket_attachments;
DROP TABLE IF EXISTS ticket_messages;
DROP TABLE IF EXISTS tickets;

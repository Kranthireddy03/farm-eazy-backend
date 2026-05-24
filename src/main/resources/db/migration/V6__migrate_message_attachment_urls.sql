-- Flyway migration: migrate existing attachment_url values
-- Copies non-empty attachment_url entries from support_ticket_messages into support_ticket_attachments
INSERT INTO support_ticket_attachments (support_ticket_message_id, file_name, url, created_at)
SELECT stm.id,
       NULLIF(SUBSTRING_INDEX(SUBSTRING_INDEX(stm.attachment_url, '?', 1), '/', -1), '') AS file_name,
       stm.attachment_url AS url,
       stm.created_at AS created_at
FROM support_ticket_messages stm
WHERE stm.attachment_url IS NOT NULL
  AND TRIM(stm.attachment_url) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM support_ticket_attachments a
    WHERE a.support_ticket_message_id = stm.id AND a.url = stm.attachment_url
  );

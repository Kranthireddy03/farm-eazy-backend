-- Flyway migration: create support_ticket_attachments table
CREATE TABLE IF NOT EXISTS support_ticket_attachments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  support_ticket_message_id BIGINT NOT NULL,
  file_name VARCHAR(512),
  url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (support_ticket_message_id) REFERENCES support_ticket_messages(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_message_id ON support_ticket_attachments(support_ticket_message_id);

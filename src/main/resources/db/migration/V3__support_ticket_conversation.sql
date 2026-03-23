-- Flyway migration: add support ticket message history
CREATE TABLE IF NOT EXISTS support_ticket_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  support_ticket_id BIGINT NOT NULL,
  sender_type VARCHAR(20) NOT NULL, -- USER, ADMIN, SYSTEM
  sender_name VARCHAR(255),
  message TEXT,
  attachment_url VARCHAR(2048),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (support_ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE
);

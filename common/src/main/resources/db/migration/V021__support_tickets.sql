CREATE TABLE support_tickets (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  category VARCHAR(32) NOT NULL,
  priority VARCHAR(16) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  description VARCHAR(4000) NOT NULL,
  status VARCHAR(24) NOT NULL,
  assigned_to VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ticket_tenant_status_time (tenant_id, status, updated_at),
  KEY idx_ticket_tenant_creator (tenant_id, created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE support_ticket_messages (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  ticket_id VARCHAR(64) NOT NULL,
  author_id VARCHAR(64) NOT NULL,
  content VARCHAR(4000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ticket_message_tenant_ticket (tenant_id, ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

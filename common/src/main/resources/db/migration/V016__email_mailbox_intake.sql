CREATE TABLE email_mailbox_configs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  email_address VARCHAR(255) NOT NULL,
  imap_host VARCHAR(255) NOT NULL,
  imap_port INT NOT NULL DEFAULT 993,
  username VARCHAR(255) NOT NULL,
  encrypted_password VARCHAR(1024) NOT NULL,
  folder_name VARCHAR(128) NOT NULL DEFAULT 'INBOX',
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  connection_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  last_sync_at TIMESTAMP NULL,
  last_error VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mailbox_tenant_email (tenant_id, email_address),
  KEY idx_mailbox_active_sync (active_flag, last_sync_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

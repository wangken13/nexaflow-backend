CREATE TABLE inbound_channel_credentials (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  channel_type VARCHAR(32) NOT NULL,
  encrypted_secret VARCHAR(1024) NOT NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  last_used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_inbound_credential_tenant_active (tenant_id, active_flag, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inbound_message_receipts (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  credential_id VARCHAR(64) NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  payload_hash CHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  inquiry_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inbound_credential_external (credential_id, external_id),
  KEY idx_inbound_receipt_tenant_received (tenant_id, received_at),
  KEY idx_inbound_receipt_inquiry (tenant_id, inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE inquiries ADD COLUMN source_channel VARCHAR(32) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE inquiries ADD COLUMN external_id VARCHAR(128);
ALTER TABLE inquiries ADD COLUMN owner_id VARCHAR(64);
ALTER TABLE inquiries ADD COLUMN next_action_due TIMESTAMP NULL;
ALTER TABLE inquiries ADD KEY idx_inquiry_tenant_owner_due (tenant_id, owner_id, next_action_due);

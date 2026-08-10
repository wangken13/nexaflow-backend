-- MySQL 8.0.31 does not support ADD COLUMN IF NOT EXISTS.
-- Flyway records this migration atomically, so these columns are added exactly once.
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
ALTER TABLE file_metadata ADD COLUMN content_type VARCHAR(128) NOT NULL DEFAULT 'application/octet-stream';

CREATE TABLE IF NOT EXISTS auth_user_sessions (
  id VARCHAR(80) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_auth_session_user_active (user_id, revoked_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS integration_outbox (
  id VARCHAR(80) PRIMARY KEY,
  aggregate_id VARCHAR(80) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  payload_json TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_outbox_pending (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_consumption (
  consumer_name VARCHAR(80) NOT NULL,
  event_id VARCHAR(80) NOT NULL,
  processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_name, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

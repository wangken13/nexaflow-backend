SET @schema_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='users' AND column_name='phone')=0,
  'ALTER TABLE users ADD COLUMN phone VARCHAR(32) NULL AFTER email', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='users' AND index_name='uk_user_phone')=0,
  'ALTER TABLE users ADD UNIQUE KEY uk_user_phone (phone)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS auth_sms_codes (
  id VARCHAR(64) PRIMARY KEY,
  phone VARCHAR(32) NOT NULL,
  purpose VARCHAR(16) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  KEY idx_sms_phone_purpose_requested (phone, purpose, requested_at),
  KEY idx_sms_active_expiry (phone, purpose, consumed_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_external_identities (
  id VARCHAR(64) PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  provider_subject VARCHAR(255) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_authenticated_at TIMESTAMP NULL,
  UNIQUE KEY uk_identity_provider_subject (provider, provider_subject),
  KEY idx_identity_user (user_id),
  KEY idx_identity_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

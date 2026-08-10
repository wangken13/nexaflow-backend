SET @schema_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='users' AND column_name='display_name')=0,
  'ALTER TABLE users ADD COLUMN display_name VARCHAR(128) NOT NULL DEFAULT '''' AFTER password_hash', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='users' AND column_name='email')=0,
  'ALTER TABLE users ADD COLUMN email VARCHAR(255) AFTER display_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='users' AND column_name='status')=0,
  'ALTER TABLE users ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''ACTIVE'' AFTER role_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='users' AND column_name='updated_at')=0,
  'ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  actor VARCHAR(64) NOT NULL,
  module VARCHAR(32) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_id VARCHAR(64),
  detail VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_tenant_created (tenant_id, created_at),
  KEY idx_audit_tenant_module (tenant_id, module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE users SET display_name = username WHERE display_name = '';
UPDATE trade_orders SET status = 'PRODUCING' WHERE status = 'PRODUCTION';

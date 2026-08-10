CREATE TABLE IF NOT EXISTS customer_contacts (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(64),
  position VARCHAR(128),
  primary_flag TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_contact_tenant_customer (tenant_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_followups (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  followup_type VARCHAR(32) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  operator_name VARCHAR(128),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_followup_tenant_customer (tenant_id, customer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  specification VARCHAR(500),
  currency VARCHAR(8) NOT NULL,
  unit_price DECIMAL(14, 4) NOT NULL,
  moq INT NOT NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_tenant_sku (tenant_id, sku),
  KEY idx_product_tenant_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quotation_items (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quotation_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64),
  product_name VARCHAR(128) NOT NULL,
  specification VARCHAR(500),
  quantity INT NOT NULL,
  unit_price DECIMAL(14, 4) NOT NULL,
  amount DECIMAL(14, 2) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  KEY idx_quotation_item_tenant_quote (tenant_id, quotation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @schema_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='quotation_no')=0,
  'ALTER TABLE quotations ADD COLUMN quotation_no VARCHAR(64)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='followup_tasks' AND column_name='related_type')=0,
  'ALTER TABLE followup_tasks ADD COLUMN related_type VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='followup_tasks' AND column_name='related_id')=0,
  'ALTER TABLE followup_tasks ADD COLUMN related_id VARCHAR(64)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='currency')=0,
  'ALTER TABLE quotations ADD COLUMN currency VARCHAR(8) NOT NULL DEFAULT ''USD''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='trade_term')=0,
  'ALTER TABLE quotations ADD COLUMN trade_term VARCHAR(16) NOT NULL DEFAULT ''FOB''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='destination_port')=0,
  'ALTER TABLE quotations ADD COLUMN destination_port VARCHAR(128)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='freight')=0,
  'ALTER TABLE quotations ADD COLUMN freight DECIMAL(14,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='valid_until')=0,
  'ALTER TABLE quotations ADD COLUMN valid_until DATE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='quotations' AND column_name='notes')=0,
  'ALTER TABLE quotations ADD COLUMN notes VARCHAR(2000)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

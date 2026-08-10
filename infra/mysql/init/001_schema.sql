CREATE TABLE IF NOT EXISTS tenants (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  plan_code VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(128) NOT NULL DEFAULT '',
  email VARCHAR(255),
  role_code VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customers (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  country VARCHAR(64),
  tag VARCHAR(32),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS inquiries (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_inquiry_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_analysis (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  inquiry_id VARCHAR(64) NOT NULL,
  intent VARCHAR(64) NOT NULL,
  urgency VARCHAR(32) NOT NULL,
  model_summary TEXT,
  next_actions_json TEXT,
  reply_draft TEXT,
  quotation_draft TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quotations (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12, 2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  quotation_no VARCHAR(64),
  currency VARCHAR(8) NOT NULL DEFAULT 'USD',
  trade_term VARCHAR(16) NOT NULL DEFAULT 'FOB',
  destination_port VARCHAR(128),
  freight DECIMAL(14, 2) NOT NULL DEFAULT 0,
  valid_until DATE,
  notes VARCHAR(2000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

CREATE TABLE IF NOT EXISTS trade_orders (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  customer_name VARCHAR(128) NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  delivery_date DATE NOT NULL,
  risk TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order_tenant_customer (tenant_id, customer_id),
  KEY idx_order_tenant_product (tenant_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS followup_tasks (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  priority VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  due_at DATETIME,
  related_type VARCHAR(32),
  related_id VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  read_flag TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS file_metadata (
  object_key VARCHAR(512) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_file_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tenants (id, name, plan_code)
VALUES ('demo-tenant', 'Demo 外贸团队', 'PRO')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (id, tenant_id, username, password_hash, display_name, role_code, status)
VALUES ('demo-admin', 'demo-tenant', 'admin', '$2a$12$FZr3HGDfWf2UAvNVUj9M5.ek.CwJvzksu7033d8bIX5TfozEEf.fe', '系统管理员', 'OWNER', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role_code = VALUES(role_code), status = VALUES(status);

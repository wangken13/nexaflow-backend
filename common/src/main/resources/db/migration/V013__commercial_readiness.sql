CREATE TABLE knowledge_articles (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  category VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  updated_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_knowledge_tenant_category (tenant_id, category, active_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE channel_configs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  channel_type VARCHAR(32) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  account_ref VARCHAR(255),
  enabled_flag TINYINT(1) NOT NULL DEFAULT 0,
  connection_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
  updated_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_tenant_type (tenant_id, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plan_catalog (
  plan_code VARCHAR(32) PRIMARY KEY,
  plan_name VARCHAR(64) NOT NULL,
  member_limit INT NOT NULL,
  customer_limit INT NOT NULL,
  ai_credit_limit INT NOT NULL,
  monthly_price DECIMAL(10,2) NOT NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO plan_catalog (plan_code, plan_name, member_limit, customer_limit, ai_credit_limit, monthly_price)
VALUES ('TRIAL', '试用版', 3, 100, 100, 0),
       ('PRO', '专业版', 20, 10000, 3000, 899),
       ('ENTERPRISE', '企业版', 200, 100000, 30000, 3999)
ON DUPLICATE KEY UPDATE plan_name=VALUES(plan_name), member_limit=VALUES(member_limit),
  customer_limit=VALUES(customer_limit), ai_credit_limit=VALUES(ai_credit_limit), monthly_price=VALUES(monthly_price);

CREATE TABLE quotation_approval_records (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quotation_id VARCHAR(64) NOT NULL,
  action VARCHAR(32) NOT NULL,
  comment_text VARCHAR(1000),
  operator_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_quote_approval_tenant_quote (tenant_id, quotation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

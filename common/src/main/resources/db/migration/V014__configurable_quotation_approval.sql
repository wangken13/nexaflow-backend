CREATE TABLE quotation_approval_rules (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  rule_type VARCHAR(32) NOT NULL,
  threshold_amount DECIMAL(14,2),
  condition_value VARCHAR(64),
  enabled_flag TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_approval_rule_tenant_enabled (tenant_id, enabled_flag, rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE quotations ADD COLUMN approval_required TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE quotations ADD COLUMN approval_reason VARCHAR(500);

ALTER TABLE ai_analysis ADD COLUMN knowledge_sufficient TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE ai_analysis ADD COLUMN knowledge_sources_json TEXT;

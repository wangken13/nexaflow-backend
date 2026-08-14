CREATE TABLE tenant_demo_records (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  batch_id VARCHAR(64) NOT NULL,
  entity_type VARCHAR(32) NOT NULL,
  entity_id VARCHAR(64) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_demo_tenant_entity (tenant_id, entity_type, entity_id),
  KEY idx_demo_tenant_batch (tenant_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

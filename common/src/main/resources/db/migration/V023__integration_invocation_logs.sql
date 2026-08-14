CREATE TABLE integration_invocation_logs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  credential_id VARCHAR(64) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  http_method VARCHAR(12) NOT NULL,
  request_path VARCHAR(255) NOT NULL,
  client_ip VARCHAR(64),
  outcome VARCHAR(24) NOT NULL,
  duration_ms BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_integration_log_tenant_created (tenant_id, created_at),
  KEY idx_integration_log_credential_created (credential_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

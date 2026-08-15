-- Completes V019 if concurrent application startup left a failed Flyway row.
CREATE TABLE IF NOT EXISTS data_import_jobs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  operator_id VARCHAR(64) NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  received_count INT NOT NULL DEFAULT 0,
  imported_count INT NOT NULL DEFAULT 0,
  skipped_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  KEY idx_import_job_tenant_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS data_import_errors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL,
  job_id VARCHAR(64) NOT NULL,
  row_number INT NOT NULL,
  error_message VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_import_error_tenant_job (tenant_id, job_id, row_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE flyway_schema_history
SET success = 1
WHERE version = '019' AND success = 0;

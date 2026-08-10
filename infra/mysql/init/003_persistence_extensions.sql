SET @model_summary_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_analysis'
    AND COLUMN_NAME = 'model_summary'
);
SET @add_model_summary = IF(
  @model_summary_exists = 0,
  'ALTER TABLE ai_analysis ADD COLUMN model_summary TEXT AFTER urgency',
  'SELECT 1'
);
PREPARE add_model_summary_stmt FROM @add_model_summary;
EXECUTE add_model_summary_stmt;
DEALLOCATE PREPARE add_model_summary_stmt;

SET @next_actions_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_analysis'
    AND COLUMN_NAME = 'next_actions_json'
);
SET @add_next_actions = IF(
  @next_actions_exists = 0,
  'ALTER TABLE ai_analysis ADD COLUMN next_actions_json TEXT AFTER model_summary',
  'SELECT 1'
);
PREPARE add_next_actions_stmt FROM @add_next_actions;
EXECUTE add_next_actions_stmt;
DEALLOCATE PREPARE add_next_actions_stmt;

CREATE TABLE IF NOT EXISTS file_metadata (
  object_key VARCHAR(512) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_file_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

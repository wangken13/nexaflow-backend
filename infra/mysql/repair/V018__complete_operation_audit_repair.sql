-- Repairs the historical V018 migration after the legacy initializer created
-- idx_operation_audit_tenant_time before Flyway managed the schema.
SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'operation_audit_logs'
      AND column_name = 'request_id'
  ),
  'SELECT 1',
  'ALTER TABLE operation_audit_logs ADD COLUMN request_id VARCHAR(64) NULL AFTER user_id'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'operation_audit_logs'
      AND column_name = 'client_ip'
  ),
  'SELECT 1',
  'ALTER TABLE operation_audit_logs ADD COLUMN client_ip VARCHAR(64) NULL AFTER request_path'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'operation_audit_logs'
      AND column_name = 'user_agent'
  ),
  'SELECT 1',
  'ALTER TABLE operation_audit_logs ADD COLUMN user_agent VARCHAR(255) NULL AFTER client_ip'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'operation_audit_logs'
      AND index_name = 'idx_operation_audit_request'
  ),
  'SELECT 1',
  'ALTER TABLE operation_audit_logs ADD INDEX idx_operation_audit_request (request_id)'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'operation_audit_logs'
      AND index_name = 'idx_operation_audit_tenant_time'
  ),
  'SELECT 1',
  'ALTER TABLE operation_audit_logs ADD INDEX idx_operation_audit_tenant_time (tenant_id, occurred_at)'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

UPDATE flyway_schema_history
SET success = 1
WHERE version = '018' AND success = 0;

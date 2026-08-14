ALTER TABLE operation_audit_logs
  ADD COLUMN request_id VARCHAR(64) NULL AFTER user_id,
  ADD COLUMN client_ip VARCHAR(64) NULL AFTER request_path,
  ADD COLUMN user_agent VARCHAR(255) NULL AFTER client_ip,
  ADD KEY idx_operation_audit_request (request_id),
  ADD KEY idx_operation_audit_tenant_time (tenant_id, occurred_at);

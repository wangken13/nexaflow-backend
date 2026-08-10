SET NAMES utf8mb4;

-- Enterprise security, normalized authorization metadata, and operational audit.
-- Order/product names and quotation item descriptions remain intentional immutable business snapshots.

CREATE TABLE IF NOT EXISTS auth_roles (
  role_code VARCHAR(32) PRIMARY KEY,
  role_name VARCHAR(64) NOT NULL,
  role_description VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_auth_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_permissions (
  permission_code VARCHAR(64) PRIMARY KEY,
  permission_name VARCHAR(128) NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_auth_permission_name (permission_name),
  KEY idx_auth_permission_module (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_role_permissions (
  role_code VARCHAR(32) NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_code, permission_code),
  KEY idx_auth_role_permission_permission (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO auth_roles (role_code, role_name, role_description) VALUES
  ('OWNER', '企业负责人', '拥有企业工作区全部管理权限'),
  ('ADMIN', '管理员', '负责团队配置和主要业务管理'),
  ('SALES', '销售人员', '负责客户、询盘、报价和跟进'),
  ('OPERATOR', '运营人员', '负责产品、订单、文件和履约'),
  ('VIEWER', '只读成员', '仅可查看授权范围内的数据')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), role_description = VALUES(role_description);

INSERT INTO auth_permissions (permission_code, permission_name, module_code) VALUES
  ('customer:read', '查看客户', 'CUSTOMER'), ('customer:write', '维护客户', 'CUSTOMER'),
  ('product:read', '查看产品', 'PRODUCT'), ('product:write', '维护产品', 'PRODUCT'),
  ('inquiry:read', '查看询盘', 'INQUIRY'), ('inquiry:write', '处理询盘', 'INQUIRY'),
  ('quotation:read', '查看报价', 'QUOTATION'), ('quotation:write', '维护报价', 'QUOTATION'),
  ('quotation:approve', '审批报价', 'QUOTATION'),
  ('order:read', '查看订单', 'ORDER'), ('order:write', '维护订单', 'ORDER'),
  ('task:read', '查看任务', 'TASK'), ('task:write', '维护任务', 'TASK'),
  ('tenant:read', '查看企业配置', 'TENANT'), ('tenant:admin', '管理企业配置', 'TENANT')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), module_code = VALUES(module_code);

INSERT IGNORE INTO auth_role_permissions (role_code, permission_code)
SELECT 'OWNER', permission_code FROM auth_permissions;
INSERT IGNORE INTO auth_role_permissions (role_code, permission_code)
SELECT 'ADMIN', permission_code FROM auth_permissions WHERE permission_code <> 'tenant:admin';
INSERT IGNORE INTO auth_role_permissions (role_code, permission_code) VALUES
  ('SALES','customer:read'),('SALES','customer:write'),('SALES','product:read'),
  ('SALES','inquiry:read'),('SALES','inquiry:write'),('SALES','quotation:read'),
  ('SALES','quotation:write'),('SALES','order:read'),('SALES','order:write'),
  ('SALES','task:read'),('SALES','task:write'),
  ('OPERATOR','customer:read'),('OPERATOR','product:read'),('OPERATOR','product:write'),
  ('OPERATOR','inquiry:read'),('OPERATOR','quotation:read'),('OPERATOR','order:read'),
  ('OPERATOR','order:write'),('OPERATOR','task:read'),('OPERATOR','task:write'),
  ('VIEWER','customer:read'),('VIEWER','product:read'),('VIEWER','inquiry:read'),
  ('VIEWER','quotation:read'),('VIEWER','order:read'),('VIEWER','task:read');

CREATE TABLE IF NOT EXISTS operation_audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  http_method VARCHAR(8) NOT NULL,
  request_path VARCHAR(255) NOT NULL,
  response_status SMALLINT NOT NULL,
  duration_ms INT UNSIGNED NOT NULL,
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_audit_tenant_time (tenant_id, occurred_at),
  KEY idx_operation_audit_user_time (user_id, occurred_at),
  KEY idx_operation_audit_status_time (response_status, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_login_attempts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  success_flag TINYINT(1) NOT NULL,
  attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_login_attempt_username_time (username, attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @schema_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='inquiries' AND index_name='idx_inquiry_tenant_customer_created')=0,
  'ALTER TABLE inquiries ADD KEY idx_inquiry_tenant_customer_created (tenant_id, customer_id, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='ai_analysis' AND index_name='idx_ai_tenant_inquiry_created')=0,
  'ALTER TABLE ai_analysis ADD KEY idx_ai_tenant_inquiry_created (tenant_id, inquiry_id, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='quotations' AND index_name='idx_quotation_tenant_customer_status')=0,
  'ALTER TABLE quotations ADD KEY idx_quotation_tenant_customer_status (tenant_id, customer_id, status, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='followup_tasks' AND index_name='idx_task_tenant_status_due')=0,
  'ALTER TABLE followup_tasks ADD KEY idx_task_tenant_status_due (tenant_id, status, due_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='notifications' AND index_name='idx_notification_tenant_read_created')=0,
  'ALTER TABLE notifications ADD KEY idx_notification_tenant_read_created (tenant_id, read_flag, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

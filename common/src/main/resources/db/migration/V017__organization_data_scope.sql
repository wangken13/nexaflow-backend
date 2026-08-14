CREATE TABLE departments (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  parent_id VARCHAR(64),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_department_tenant_name (tenant_id, name),
  KEY idx_department_tenant_parent (tenant_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE users
  ADD COLUMN department_id VARCHAR(64) NULL AFTER email,
  ADD COLUMN data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF' AFTER role_code,
  ADD KEY idx_user_tenant_department (tenant_id, department_id);

ALTER TABLE customers
  ADD COLUMN owner_id VARCHAR(64) NULL AFTER tag,
  ADD COLUMN department_id VARCHAR(64) NULL AFTER owner_id,
  ADD KEY idx_customer_tenant_owner (tenant_id, owner_id),
  ADD KEY idx_customer_tenant_department (tenant_id, department_id);

UPDATE users
SET data_scope = CASE
  WHEN role_code IN ('OWNER', 'ADMIN') THEN 'ALL'
  WHEN role_code = 'OPERATOR' THEN 'DEPARTMENT'
  ELSE 'SELF'
END;

UPDATE customers customer_record
SET owner_id = (
  SELECT member.id FROM users member
  WHERE member.tenant_id = customer_record.tenant_id AND member.role_code = 'OWNER'
  ORDER BY member.created_at LIMIT 1
)
WHERE owner_id IS NULL;

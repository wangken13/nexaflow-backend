-- The application uses logical foreign keys. Relationship consistency is
-- validated in the service layer and supported by ordinary lookup indexes.
SET @schema_name = DATABASE();

SELECT GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', constraint_name, '`') SEPARATOR ', ')
INTO @drop_foreign_keys
FROM information_schema.key_column_usage
WHERE table_schema = @schema_name
  AND table_name = 'auth_role_permissions'
  AND referenced_table_name IS NOT NULL;

SET @sql = IF(
  @drop_foreign_keys IS NULL OR @drop_foreign_keys = '',
  'SELECT 1',
  CONCAT('ALTER TABLE auth_role_permissions ', @drop_foreign_keys)
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @schema_name
     AND table_name = 'auth_role_permissions'
     AND index_name = 'idx_auth_role_permission_permission') = 0,
  'ALTER TABLE auth_role_permissions ADD KEY idx_auth_role_permission_permission (permission_code)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

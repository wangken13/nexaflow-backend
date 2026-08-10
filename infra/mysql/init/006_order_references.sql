SET @schema_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='trade_orders' AND column_name='customer_id')=0,
  'ALTER TABLE trade_orders ADD COLUMN customer_id VARCHAR(64) NOT NULL DEFAULT '''' AFTER tenant_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='trade_orders' AND column_name='product_id')=0,
  'ALTER TABLE trade_orders ADD COLUMN product_id VARCHAR(64) NOT NULL DEFAULT '''' AFTER customer_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE trade_orders orders
JOIN customers customer ON customer.tenant_id = orders.tenant_id AND customer.name = orders.customer_name
JOIN products product ON product.tenant_id = orders.tenant_id AND product.name = orders.product_name
SET orders.customer_id = customer.id,
    orders.product_id = product.id
WHERE orders.customer_id = '' OR orders.product_id = '';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='trade_orders' AND index_name='idx_order_tenant_customer')=0,
  'ALTER TABLE trade_orders ADD KEY idx_order_tenant_customer (tenant_id, customer_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='trade_orders' AND index_name='idx_order_tenant_product')=0,
  'ALTER TABLE trade_orders ADD KEY idx_order_tenant_product (tenant_id, product_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

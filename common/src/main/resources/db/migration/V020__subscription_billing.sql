CREATE TABLE subscription_orders (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  plan_code VARCHAR(32) NOT NULL,
  billing_months INT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  provider_reference VARCHAR(128),
  provider_transaction_id VARCHAR(128),
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL,
  expires_at TIMESTAMP NOT NULL,
  KEY idx_subscription_order_tenant_time (tenant_id, created_at),
  KEY idx_subscription_order_status_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_requests (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  invoice_title VARCHAR(255) NOT NULL,
  tax_number VARCHAR(64),
  recipient_email VARCHAR(255) NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_invoice_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refund_requests (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_refund_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

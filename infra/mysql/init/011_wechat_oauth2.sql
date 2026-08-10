-- OAuth 2.0 authorization state and short-lived frontend login tickets.
-- Associations with users are logical foreign keys; no MySQL FOREIGN KEY is used.
CREATE TABLE IF NOT EXISTS auth_oauth_states (
  state VARCHAR(64) PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_oauth_state_provider_expiry (provider, expires_at),
  KEY idx_oauth_state_active (consumed_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_wechat_login_tickets (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wechat_ticket_user (user_id),
  KEY idx_wechat_ticket_active (consumed_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

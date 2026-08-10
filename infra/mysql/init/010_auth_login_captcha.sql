-- Server-issued account-login captcha. No physical foreign keys are used.
CREATE TABLE IF NOT EXISTS auth_login_captchas (
  id VARCHAR(64) PRIMARY KEY,
  answer_hash VARCHAR(255) NOT NULL,
  attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_login_captcha_expiry (expires_at),
  KEY idx_login_captcha_active (consumed_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

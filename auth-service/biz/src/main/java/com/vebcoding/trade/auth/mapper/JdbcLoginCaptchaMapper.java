package com.vebcoding.trade.auth.mapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLoginCaptchaMapper implements LoginCaptchaMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginCaptchaMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(LoginCaptcha captcha) {
        jdbcTemplate.update("""
                INSERT INTO auth_login_captchas (id, answer_hash, expires_at, attempts)
                VALUES (?, ?, ?, ?)
                """, captcha.id(), captcha.answerHash(), Timestamp.from(captcha.expiresAt()), captcha.attempts());
    }

    @Override
    public Optional<LoginCaptcha> findActiveById(String id, Instant now) {
        return jdbcTemplate.query("""
                SELECT id, answer_hash, expires_at, attempts
                FROM auth_login_captchas
                WHERE id = ? AND consumed_at IS NULL AND expires_at > ?
                """, (rs, rowNum) -> new LoginCaptcha(
                rs.getString("id"), rs.getString("answer_hash"), rs.getTimestamp("expires_at").toInstant(),
                rs.getInt("attempts")), id, Timestamp.from(now)).stream().findFirst();
    }

    @Override
    public boolean consume(String id) {
        return jdbcTemplate.update("""
                UPDATE auth_login_captchas
                SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND consumed_at IS NULL AND attempts < 5
                """, id) == 1;
    }

    @Override
    public void increaseAttempts(String id) {
        jdbcTemplate.update("UPDATE auth_login_captchas SET attempts = attempts + 1 WHERE id = ? AND attempts < 5", id);
    }

    @Override
    public void purgeExpired(Instant before) {
        jdbcTemplate.update("DELETE FROM auth_login_captchas WHERE expires_at < ?", Timestamp.from(before));
    }
}

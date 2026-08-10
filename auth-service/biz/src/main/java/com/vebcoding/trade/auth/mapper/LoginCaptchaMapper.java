package com.vebcoding.trade.auth.mapper;

import java.time.Instant;
import java.util.Optional;

public interface LoginCaptchaMapper {
    void save(LoginCaptcha captcha);

    Optional<LoginCaptcha> findActiveById(String id, Instant now);

    boolean consume(String id);

    void increaseAttempts(String id);

    void purgeExpired(Instant before);

    record LoginCaptcha(String id, String answerHash, Instant expiresAt, int attempts) {
    }
}

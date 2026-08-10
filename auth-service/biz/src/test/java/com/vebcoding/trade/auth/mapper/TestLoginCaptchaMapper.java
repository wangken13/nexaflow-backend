package com.vebcoding.trade.auth.mapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TestLoginCaptchaMapper implements LoginCaptchaMapper {
    private final Map<String, LoginCaptcha> values = new ConcurrentHashMap<>();
    private final Set<String> consumed = ConcurrentHashMap.newKeySet();

    @Override
    public void save(LoginCaptcha captcha) {
        values.put(captcha.id(), captcha);
    }

    @Override
    public Optional<LoginCaptcha> findActiveById(String id, Instant now) {
        return Optional.ofNullable(values.get(id))
                .filter(captcha -> !consumed.contains(id) && captcha.expiresAt().isAfter(now));
    }

    @Override
    public boolean consume(String id) {
        return values.containsKey(id) && consumed.add(id);
    }

    @Override
    public void increaseAttempts(String id) {
        values.computeIfPresent(id, (key, captcha) -> new LoginCaptcha(captcha.id(), captcha.answerHash(),
                captcha.expiresAt(), captcha.attempts() + 1));
    }

    @Override
    public void purgeExpired(Instant before) {
        values.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(before));
    }

    public int attempts(String id) {
        return values.get(id).attempts();
    }
}

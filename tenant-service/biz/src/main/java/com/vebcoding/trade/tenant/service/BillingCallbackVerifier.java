package com.vebcoding.trade.tenant.service;

import com.vebcoding.trade.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BillingCallbackVerifier {
    private final String secret;

    public BillingCallbackVerifier(@Value("${app.billing.webhook-secret:}") String secret) {
        this.secret = secret == null ? "" : secret.trim();
    }

    public void verify(String timestamp, String body, String signature) {
        if (secret.length() < 32) throw BusinessException.unavailable("支付回调密钥尚未配置");
        Instant requestTime;
        try { requestTime = Instant.ofEpochSecond(Long.parseLong(timestamp)); }
        catch (Exception exception) { throw BusinessException.unauthorized("支付回调时间戳无效"); }
        if (Duration.between(requestTime, Instant.now()).abs().toMinutes() > 5) {
            throw BusinessException.unauthorized("支付回调已过期");
        }
        byte[] expected = HexFormat.of().parseHex(hmac(timestamp + "." + body));
        byte[] supplied;
        try { supplied = HexFormat.of().parseHex(signature == null ? "" : signature); }
        catch (IllegalArgumentException exception) { throw BusinessException.unauthorized("支付回调签名无效"); }
        if (!MessageDigest.isEqual(expected, supplied)) throw BusinessException.unauthorized("支付回调签名无效");
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("Unable to verify billing callback", exception); }
    }
}

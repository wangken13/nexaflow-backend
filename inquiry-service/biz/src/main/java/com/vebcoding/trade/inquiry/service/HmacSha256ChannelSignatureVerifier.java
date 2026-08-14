package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
class HmacSha256ChannelSignatureVerifier implements ChannelSignatureVerifier {
    private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(5);
    private final Clock clock;

    HmacSha256ChannelSignatureVerifier() {
        this(Clock.systemUTC());
    }

    HmacSha256ChannelSignatureVerifier(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void verify(String secret, String timestamp, String body, String suppliedSignature) {
        long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestamp == null ? "" : timestamp.trim());
        } catch (NumberFormatException exception) {
            throw BusinessException.unauthorized("渠道请求时间戳无效");
        }
        Instant requestTime = Instant.ofEpochSecond(epochSeconds);
        if (Duration.between(requestTime, clock.instant()).abs().compareTo(ALLOWED_CLOCK_SKEW) > 0) {
            throw BusinessException.unauthorized("渠道请求已过期，请校准系统时间后重试");
        }
        String expected = sign(secret, epochSeconds + "." + body);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        byte[] suppliedBytes = normalize(suppliedSignature).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expectedBytes, suppliedBytes)) {
            throw BusinessException.unauthorized("渠道请求签名校验失败");
        }
    }

    static String sign(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private String normalize(String signature) {
        String normalized = signature == null ? "" : signature.trim().toLowerCase();
        return normalized.startsWith("sha256=") ? normalized.substring(7) : normalized;
    }
}

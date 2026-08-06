package com.vebcoding.trade.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class JwtSupport {
    private JwtSupport() {
    }

    public static String create(String subject, String tenantId, String role, String secret, long ttlSeconds) {
        String header = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = base64("{\"sub\":\"%s\",\"tenantId\":\"%s\",\"role\":\"%s\",\"exp\":%d}"
                .formatted(escape(subject), escape(tenantId), escape(role), exp));
        String signature = sign(header + "." + payload, secret);
        return header + "." + payload + "." + signature;
    }

    public static Map<String, String> verify(String token, String secret) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3 || !sign(parts[0] + "." + parts[1], secret).equals(parts[2])) {
            throw new IllegalArgumentException("Invalid token");
        }
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, String> claims = parseFlatJson(json);
        long exp = Long.parseLong(claims.getOrDefault("exp", "0"));
        if (exp < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("Token expired");
        }
        return claims;
    }

    private static String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign token", ex);
        }
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, String> parseFlatJson(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        String body = json.substring(1, json.length() - 1);
        for (String part : body.split(",")) {
            String[] pair = part.split(":", 2);
            if (pair.length == 2) {
                String key = pair[0].replace("\"", "").trim();
                String value = pair[1].replace("\"", "").trim();
                result.put(key, value);
            }
        }
        return result;
    }
}

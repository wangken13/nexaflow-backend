package com.vebcoding.trade.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class JwtSupport {
    private JwtSupport() {
    }

    public static String create(String subject, String tenantId, String role, String secret, long ttlSeconds) {
        return createAccess(subject, tenantId, role, UUID.randomUUID().toString(), secret, ttlSeconds);
    }

    public static String createAccess(String subject, String tenantId, String role, String sessionId, String secret, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("tenantId", tenantId)
                .claim("role", role)
                .claim("sid", sessionId)
                .claim("typ", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key(secret))
                .compact();
    }

    public static String createRefresh(String subject, String sessionId, String secret, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(subject).claim("sid", sessionId)
                .claim("typ", "REFRESH").issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key(secret)).compact();
    }

    public static Map<String, String> verify(String token, String secret) {
        Claims claims = Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(token).getPayload();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("sub", claims.getSubject());
        result.put("tenantId", claims.get("tenantId", String.class));
        result.put("role", claims.get("role", String.class));
        result.put("sid", claims.get("sid", String.class));
        result.put("typ", claims.get("typ", String.class));
        result.put("jti", claims.getId());
        result.put("exp", String.valueOf(claims.getExpiration().toInstant().getEpochSecond()));
        return result;
    }

    public static void validateSecret(String secret) {
        key(secret);
    }

    private static SecretKey key(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

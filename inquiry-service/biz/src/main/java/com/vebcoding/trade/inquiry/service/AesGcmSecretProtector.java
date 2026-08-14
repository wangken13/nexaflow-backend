package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class AesGcmSecretProtector implements SecretProtector {
    private static final int IV_BYTES = 12;
    private final String masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    AesGcmSecretProtector(@Value("${app.integration.master-key:}") String masterKey) {
        this.masterKey = masterKey == null ? "" : masterKey.trim();
    }

    @Override
    public String protect(String secret) {
        requireConfiguration();
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception exception) {
            throw BusinessException.unavailable("渠道密钥加密失败，请检查接入密钥配置");
        }
    }

    @Override
    public String reveal(String protectedSecret) {
        requireConfiguration();
        try {
            byte[] payload = Base64.getUrlDecoder().decode(protectedSecret);
            if (payload.length <= IV_BYTES) throw new IllegalArgumentException("invalid encrypted secret");
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw BusinessException.unavailable("渠道密钥无法解密，请联系管理员检查配置");
        }
    }

    @Override
    public boolean available() {
        return masterKey.length() >= 32;
    }

    private SecretKeySpec key() throws Exception {
        return new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                .digest(masterKey.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    private void requireConfiguration() {
        if (!available()) throw BusinessException.unavailable("渠道接入尚未配置，请设置 INTEGRATION_MASTER_KEY");
    }
}

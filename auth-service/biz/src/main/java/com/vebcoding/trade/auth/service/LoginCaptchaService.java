package com.vebcoding.trade.auth.service;

import com.vebcoding.trade.auth.api.LoginCaptchaResponse;
import com.vebcoding.trade.auth.mapper.LoginCaptchaMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginCaptchaService implements LoginCaptchaVerifier {
    private static final int CAPTCHA_TTL_SECONDS = 120;
    private static final int MAX_ATTEMPTS = 5;
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private final LoginCaptchaMapper captchaMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginCaptchaService(LoginCaptchaMapper captchaMapper, PasswordEncoder passwordEncoder) {
        this.captchaMapper = captchaMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginCaptchaResponse issue() {
        Instant now = Instant.now();
        String answer = nextAnswer();
        String captchaId = "captcha-" + UUID.randomUUID();
        captchaMapper.purgeExpired(now.minusSeconds(86_400));
        captchaMapper.save(new LoginCaptchaMapper.LoginCaptcha(captchaId, passwordEncoder.encode(answer),
                now.plusSeconds(CAPTCHA_TTL_SECONDS), 0));
        return new LoginCaptchaResponse(captchaId, pngDataUrl(answer), CAPTCHA_TTL_SECONDS);
    }

    @Override
    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) return false;
        Optional<LoginCaptchaMapper.LoginCaptcha> captcha = captchaMapper.findActiveById(captchaId, Instant.now());
        if (captcha.isEmpty() || captcha.get().attempts() >= MAX_ATTEMPTS) return false;
        if (!passwordEncoder.matches(captchaCode.trim().toUpperCase(), captcha.get().answerHash())) {
            captchaMapper.increaseAttempts(captchaId);
            return false;
        }
        return captchaMapper.consume(captchaId);
    }

    private String nextAnswer() {
        StringBuilder answer = new StringBuilder(5);
        for (int index = 0; index < 5; index++) answer.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        return answer.toString();
    }

    private String pngDataUrl(String answer) {
        BufferedImage image = new BufferedImage(180, 52, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 243, 255));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setStroke(new BasicStroke(1.3f));
            for (int index = 0; index < 8; index++) {
                graphics.setColor(new Color(124, 58, 237, 70 + secureRandom.nextInt(70)));
                graphics.drawLine(secureRandom.nextInt(180), secureRandom.nextInt(52),
                        secureRandom.nextInt(180), secureRandom.nextInt(52));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
            for (int index = 0; index < answer.length(); index++) {
                int x = 18 + index * 31;
                int y = 35 + secureRandom.nextInt(7) - 3;
                graphics.setColor(new Color(63 + secureRandom.nextInt(45), 28, 115 + secureRandom.nextInt(65)));
                double rotation = Math.toRadians(secureRandom.nextInt(25) - 12);
                graphics.rotate(rotation, x, y);
                graphics.drawString(String.valueOf(answer.charAt(index)), x, y);
                graphics.rotate(-rotation, x, y);
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", output);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("验证码图片生成失败", exception);
        } finally {
            graphics.dispose();
        }
    }
}

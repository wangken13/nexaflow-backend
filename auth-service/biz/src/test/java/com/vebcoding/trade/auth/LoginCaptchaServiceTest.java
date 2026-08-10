package com.vebcoding.trade.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.auth.mapper.LoginCaptchaMapper;
import com.vebcoding.trade.auth.mapper.TestLoginCaptchaMapper;
import com.vebcoding.trade.auth.service.LoginCaptchaService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LoginCaptchaServiceTest {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TestLoginCaptchaMapper mapper = new TestLoginCaptchaMapper();
    private final LoginCaptchaService service = new LoginCaptchaService(mapper, passwordEncoder);

    @Test
    void issuedCaptchaHasOpaqueIdAndRasterImage() {
        var captcha = service.issue();

        assertThat(captcha.captchaId()).startsWith("captcha-");
        assertThat(captcha.imageDataUrl()).startsWith("data:image/png;base64,");
        assertThat(captcha.expiresInSeconds()).isEqualTo(120);
    }

    @Test
    void validCaptchaCanOnlyBeConsumedOnce() {
        mapper.save(new LoginCaptchaMapper.LoginCaptcha("captcha-test", passwordEncoder.encode("AB234"),
                Instant.now().plusSeconds(60), 0));

        assertThat(service.verify("captcha-test", "ab234")).isTrue();
        assertThat(service.verify("captcha-test", "AB234")).isFalse();
    }

    @Test
    void invalidCaptchaRecordsAttempt() {
        mapper.save(new LoginCaptchaMapper.LoginCaptcha("captcha-invalid", passwordEncoder.encode("AB234"),
                Instant.now().plusSeconds(60), 0));

        assertThat(service.verify("captcha-invalid", "WRONG")).isFalse();
        assertThat(mapper.attempts("captcha-invalid")).isEqualTo(1);
    }
}
